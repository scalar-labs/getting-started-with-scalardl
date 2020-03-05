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

public class AssetHistoryContract extends Contract {

  public static final String ID = "id";
  public static final String TIMESTAMP = "timestamp";
  public static final String STATUS = "status";
  public static final String AGE = "age";
  public static final String HISTORY = "history";
  public static final String HOLDER_ID = "holderId";

  private static final String RESULT = "result";
  private static final String SUCCESS = "success";
  private static final String FAILURE = "failure";
  private static final String MESSAGE = "message";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!argument.containsKey(ID)) {
      throw new ContractContextException("wrong argument.");
    }
    if (!property.isPresent() || !property.get().containsKey(HOLDER_ID)) {
      throw new ContractContextException("property: `" + HOLDER_ID + "` is mandatory.");
    }

    String holderId = property.get().getString(HOLDER_ID);
    String id = argument.getString(ID);

    AssetFilter filter = new AssetFilter(holderId + "-" + id);
    List<Asset> borrowingHistory = ledger.scan(filter);

    if (borrowingHistory.isEmpty()) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "This asset is not registered")
          .build();
    }

    JsonArrayBuilder builder = Json.createArrayBuilder();
    JsonObjectBuilder borrowingRecordBuilder = Json.createObjectBuilder();
    for (Asset history : borrowingHistory) {
      borrowingRecordBuilder
          .add(TIMESTAMP, history.data().getJsonNumber(TIMESTAMP).longValue())
          .add(STATUS, history.data().getString(STATUS))
          .add(AGE, history.age());

      if (history.data().containsKey(HOLDER_ID)) {
        borrowingRecordBuilder.add(HOLDER_ID, history.data().getString(HOLDER_ID));
      }
      builder.add(borrowingRecordBuilder.build());
    }

    return Json.createObjectBuilder()
        .add(RESULT, SUCCESS)
        .add(MESSAGE, "get history complete.")
        .add(HISTORY, builder.build())
        .build();
  }
}
