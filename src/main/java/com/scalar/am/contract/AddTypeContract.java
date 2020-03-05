package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;

public class AddTypeContract extends Contract {

  public static final String NAME = "name";
  public static final String TYPE = "type";
  public static final String HOLDER_ID = "holderId";

  private static final String RESULT = "result";
  private static final String SUCCESS = "success";
  private static final String FAILURE = "failure";
  private static final String MESSAGE = "message";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!argument.containsKey(NAME)) {
      throw new ContractContextException("wrong argument.");
    }
    if (!property.isPresent() || !property.get().containsKey(HOLDER_ID)) {
      throw new ContractContextException("property: `" + HOLDER_ID + "` is mandatory.");
    }

    String holderId = property.get().getString(HOLDER_ID);
    String name = argument.getString(NAME);

    Optional<Asset> type = ledger.get(holderId + "-" + name);
    if (type.isPresent()) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "Type " + name + " is already registered.")
          .build();
    }

    ledger.put(holderId + "-" + name, Json.createObjectBuilder().build());

    JsonObject newType = Json.createObjectBuilder().add(NAME, name).build();
    ledger.get(holderId + "-" + TYPE);
    ledger.put(holderId + "-" + TYPE, newType);

    return Json.createObjectBuilder()
        .add(RESULT, SUCCESS)
        .add(MESSAGE, "type " + name + " put completed.")
        .build();
  }
}
