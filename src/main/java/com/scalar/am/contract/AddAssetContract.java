package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class AddAssetContract extends Contract {

  public static final String TYPE = "type";
  public static final String ASSET = "asset";
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String TIMESTAMP = "timestamp";
  public static final String STATUS = "status";
  public static final String IN_STOCK = "in-stock";
  public static final String HOLDER_ID = "holderId";

  private static final String RESULT = "result";
  private static final String SUCCESS = "success";
  private static final String FAILURE = "failure";
  private static final String MESSAGE = "message";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!argument.containsKey(TYPE)
        || !argument.containsKey(ASSET)
        || !argument.containsKey(TIMESTAMP)
        || !argument.containsKey(ID)) {
      throw new ContractContextException("wrong argument.");
    }

    if (!property.isPresent() || !property.get().containsKey(HOLDER_ID)) {
      throw new ContractContextException("property: `" + HOLDER_ID + "` is mandatory.");
    }

    String holderId = property.get().getString(HOLDER_ID);
    String type = argument.getString(TYPE);
    String name = argument.getString(ASSET);
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();
    String id = argument.getString(ID);

    Optional<Asset> optAsset = ledger.get(holderId + "-" + type);

    if (!optAsset.isPresent()) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "Type " + type + " not found. Use am add-type to create it.")
          .build();
    }

    optAsset = ledger.get(holderId + "-" + id);

    if (optAsset.isPresent()) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "This asset is already registered.")
          .build();
    }

    JsonObject assetStatusJson =
        Json.createObjectBuilder().add(TIMESTAMP, timestamp).add(STATUS, IN_STOCK).build();
    ledger.put(holderId + "-" + id, assetStatusJson);

    JsonObject assetNameJson = Json.createObjectBuilder().add(ID, id).add(NAME, name).build();
    ledger.put(holderId + "-" + type, assetNameJson);

    return Json.createObjectBuilder()
        .add(RESULT, SUCCESS)
        .add(MESSAGE, "asset " + name + " put completed.")
        .build();
  }
}
