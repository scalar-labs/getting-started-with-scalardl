package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ListContract extends Contract {

  public static final String TYPE = "type";
  public static final String ID = "id";
  public static final String STATUS = "status";
  public static final String NAME = "name";
  public static final String TIMESTAMP = "timestamp";
  public static final String HOLDER_ID = "holderId";

  private static final String RESULT = "result";
  private static final String SUCCESS = "success";
  private static final String FAILURE = "failure";
  private static final String MESSAGE = "message";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!argument.containsKey(TYPE)) {
      throw new ContractContextException("wrong argument.");
    }
    if (!property.isPresent() || !property.get().containsKey(HOLDER_ID)) {
      throw new ContractContextException("property: `" + HOLDER_ID + "` is mandatory.");
    }

    String holderId = property.get().getString(HOLDER_ID);
    String type = argument.getString(TYPE);
    AssetFilter filter = new AssetFilter(holderId + "-" + type);

    List<Asset> assetList = ledger.scan(filter);

    if (assetList.isEmpty()) { // means the type is not created yet
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "Type " + type + " not found. Use am add-type to create it.")
          .build();
    }

    /*
     * The history of key: <type> is a list. E.g. history of book = [ {}, // initiated value when
     * book is created (age: 0) { "id": "ff02d8f3e5a85419df55ced87bf1bd68", "name":
     * "Java in a nutshell" }, // the first added book (age: 1) { "id":
     * "d41d8cd98f00b204e9800998ecf8427e", "name": "Java the good part" }, the second added book
     * (age: 2) ]
     *
     * For each book we have another key: book_<id> to store its borrowing status E.g. history of
     * book_ff02d8f3e5a85419df55ced87bf1bd68 = [ { "timestamp": ..., "status": "in-stock" }, // age:
     * 0 { "timestamp": ..., "status": "on-loan" }, // age: 1 ]
     */
    JsonArrayBuilder assetsBuilder = Json.createArrayBuilder();
    for (Asset asset : assetList) {
      JsonObject data = asset.data();
      if (data.size() == 0) { // initiated one, ignore it
        continue;
      }
      String id = data.getString(ID);
      String name = data.getString(NAME);

      Optional<Asset> borrowingStatus = ledger.get(holderId + "-" + id);
      if (!borrowingStatus.isPresent()) {
        /**
         * Abnormal case. We found an asset in list but no borrowing status record. Just ignore it
         */
        continue;
      }

      JsonObjectBuilder statusBuilder = Json.createObjectBuilder();
      statusBuilder
          .add(ID, id)
          .add(NAME, name)
          .add(TIMESTAMP, borrowingStatus.get().data().getJsonNumber(TIMESTAMP).longValue())
          .add(STATUS, borrowingStatus.get().data().getString(STATUS));

      if (borrowingStatus.get().data().containsKey(HOLDER_ID)) {
        statusBuilder.add(HOLDER_ID, borrowingStatus.get().data().getString(HOLDER_ID));
      }

      assetsBuilder.add(statusBuilder.build());
    }

    return Json.createObjectBuilder()
        .add(RESULT, SUCCESS)
        .add(MESSAGE, "get list completed.")
        .add(type, assetsBuilder.build())
        .build();
  }
}
