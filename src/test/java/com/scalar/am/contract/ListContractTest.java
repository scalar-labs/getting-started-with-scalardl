package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.database.AssetFilter;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ListContractTest extends ContractTest {

  private ListContract contract = new ListContract();
  private JsonObject argument;
  private JsonObject property;
  private JsonObject dataRecords;
  private List<Asset> list = new ArrayList<Asset>();

  @Mock private Ledger ledger;
  @Mock private Asset asset;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    argument = Json.createObjectBuilder().add(ListContract.TYPE, TYPE_EX).build();
    property =
        Json.createObjectBuilder().add(AssetHistoryContract.HOLDER_ID, HOLDER_ID_1_EX).build();
  }

  private void setStatusRecords() {

    Mockito.when(asset.id()).thenReturn(ID_EX);
    Mockito.when(asset.age()).thenReturn(AGE_EX);

    dataRecords =
        Json.createObjectBuilder()
            .add(ListContract.ID, ID_EX)
            .add(ListContract.NAME, NAME_EX)
            .add(ListContract.TIMESTAMP, TIMESTAMP_EX)
            .add(ListContract.STATUS, IN_STOCK)
            .add(ListContract.HOLDER_ID, HOLDER_ID_1_EX)
            .build();
    Mockito.when(asset.data()).thenReturn(dataRecords);

    list.add(asset);
  }

  @Test
  public void invoke_ArgumentWasNull_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().build();
    thrown.expect(ContractContextException.class);
    thrown.expectMessage("wrong argument.");
    // Act
    contract.invoke(ledger, argument, Optional.empty());
  }

  @Test
  public void invoke_PropertyWasNull_ShouldThrowContractContextException() {
    // Arrange
    JsonObject property = Json.createObjectBuilder().build();
    thrown.expect(ContractContextException.class);
    thrown.expectMessage("property: `" + ListContract.HOLDER_ID + "` is mandatory.");
    // Act
    contract.invoke(ledger, argument, Optional.of(property));
  }

  @Test
  public void invoke_QueryNonExistingType_ShouldReturnFailure() {
    // Arrange
    Mockito.when(ledger.scan(Mockito.any(AssetFilter.class))).thenReturn(new ArrayList<Asset>());
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_QueryBookList_ShouldReturnProperly() {
    // Arrange
    setStatusRecords();
    Mockito.when(ledger.scan(Mockito.any(AssetFilter.class))).thenReturn(list);
    Mockito.when(ledger.get(HOLDER_ID_1_EX + "-" + ID_EX)).thenReturn(Optional.of(asset));
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Assert.assertEquals(SUCCESS, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));

    JsonArray assets = response.getJsonArray(TYPE_EX);
    Assert.assertEquals(1, assets.size());

    JsonObject asset = assets.getJsonObject(0);
    Assert.assertEquals(ID_EX, asset.getString(ListContract.ID));
    Assert.assertEquals(NAME_EX, asset.getString(ListContract.NAME));
    Assert.assertEquals(TIMESTAMP_EX, asset.getJsonNumber(ListContract.TIMESTAMP).longValue());
    Assert.assertEquals(IN_STOCK, asset.getString(ListContract.STATUS));
    Assert.assertEquals(HOLDER_ID_1_EX, asset.getString(ListContract.HOLDER_ID));
  }
}
