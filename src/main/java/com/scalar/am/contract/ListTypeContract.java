package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.contract.Contract;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ListTypeContract extends Contract {

  public static final String TYPE = "type";
  public static final String AGE = "age";
  public static final String TYPES = "types";
  public static final String NAME = "name";
  public static final String HOLDER_ID = "holderId";

  private static final String RESULT = "result";
  private static final String SUCCESS = "success";
  private static final String FAILURE = "failure";
  private static final String MESSAGE = "message";

  @Override
  public JsonObject invoke(Ledger ledger, JsonObject argument, Optional<JsonObject> property) {
    if (!property.isPresent() || !property.get().containsKey(HOLDER_ID)) {
      throw new ContractContextException("property: `" + HOLDER_ID + "` is mandatory.");
    }

    String holderId = property.get().getString(HOLDER_ID);

    /**
     * Registered types will be put into key: type, thus the history of key: type is just the list
     * of registered types E.g. history of type = [ { "name": "book" }, // age: 0 { "name": "phone"
     * } // age: 1 ]
     */
    AssetFilter filter = new AssetFilter(holderId + "-" + TYPE);
    List<Asset> history = ledger.scan(filter);
    if (history.isEmpty()) {
      return Json.createObjectBuilder()
          .add(RESULT, FAILURE)
          .add(MESSAGE, "No types were registered. Use am add-type to create one.")
          .build();
    }

    JsonArrayBuilder builder = Json.createArrayBuilder();
    for (Asset h : history) {
      JsonObject type =
          Json.createObjectBuilder().add(TYPE, h.data().getString(NAME)).add(AGE, h.age()).build();
      builder.add(type);
    }
    JsonArray types = builder.build();

    return Json.createObjectBuilder()
        .add(RESULT, SUCCESS)
        .add(MESSAGE, "get list completed.")
        .add(TYPES, types)
        .build();
  }
}
