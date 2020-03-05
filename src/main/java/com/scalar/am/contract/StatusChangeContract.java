package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class StatusChangeContract extends Contract {

  public static final String ASSET_ID = "asset_id";
  public static final String TIMESTAMP = "timestamp";
  public static final String STATUS = "status";
  public static final String HOLDER_ID = "holderId";

  private static final String RESULT = "result";
  private static final String SUCCESS = "success";
  private static final String FAILURE = "failure";
  private static final String MESSAGE = "message";

  public static final String ON_LOAN = "on-loan";
  public static final String IN_STOCK = "in-stock";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!argument.containsKey(ASSET_ID)
        || !argument.containsKey(STATUS)
        || !argument.containsKey(TIMESTAMP)) {
      throw new ContractContextException("wrong argument.");
    }
    if (!property.isPresent() || !property.get().containsKey(HOLDER_ID)) {
      throw new ContractContextException("property: `" + HOLDER_ID + "` is mandatory.");
    }

    String holderId = property.get().getString(HOLDER_ID);
    String id = argument.getString(ASSET_ID);
    String newStatus = argument.getString(STATUS);
    long timestamp = argument.getJsonNumber(TIMESTAMP).longValue();

    Optional<Asset> asset = ledger.get(holderId + "-" + id);
    if (!asset.isPresent()) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "Asset not found.")
          .build();
    }

    JsonObject data = asset.get().data();
    if (!data.containsKey(STATUS)) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, String.format("%s is not an asset", id))
          .build();
    }
    if (data.getString(STATUS).equals(newStatus)) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(
              MESSAGE,
              String.format(
                  "Asset is already %s.", ON_LOAN.equals(newStatus) ? "borrowed" : "returned"))
          .build();
    }

    if (!property.isPresent()) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "The property is missing")
          .build();
    }
    if (!property.get().containsKey(HOLDER_ID)) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "The property `holderId` is missing")
          .build();
    }

    JsonObjectBuilder newDataBuilder = Json.createObjectBuilder();
    if (newStatus.equals(ON_LOAN)) {
      newDataBuilder.add(HOLDER_ID, holderId).add(TIMESTAMP, timestamp).add(STATUS, newStatus);
    } else if (newStatus.equals(IN_STOCK)) {
      if (!data.containsKey(HOLDER_ID)) {
        return Json.createObjectBuilder()
            .add(RESULT, FAILURE)
            .add(MESSAGE, "Can not return asset without holderId")
            .build();
      }
      if (!data.getString(HOLDER_ID).equals(holderId)) {
        return Json.createObjectBuilder()
            .add(RESULT, FAILURE)
            .add(MESSAGE, "Can not return asset borrowed by another user")
            .build();
      }
      newDataBuilder.add(TIMESTAMP, timestamp).add(STATUS, newStatus);
    }
    ledger.put(holderId + "-" + id, newDataBuilder.build());
    return Json.createObjectBuilder()
        .add(RESULT, SUCCESS)
        .add(MESSAGE, ON_LOAN.equals(newStatus) ? "Borrowed" : "Returned")
        .build();
  }
}
