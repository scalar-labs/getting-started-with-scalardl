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

public class ListTypeContractTest extends ContractTest {

  private ListTypeContract contract = new ListTypeContract();
  private JsonObject argument;
  private JsonObject property;
  private JsonObject dataRecords;
  private List<Asset> historyList = new ArrayList<Asset>();

  @Mock private Ledger ledger;
  @Mock private Asset asset;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    argument = Json.createObjectBuilder().add(ListTypeContract.TYPE, TYPE_EX).build();
    property =
        Json.createObjectBuilder().add(AssetHistoryContract.HOLDER_ID, HOLDER_ID_1_EX).build();
  }

  private void setAssetRecords() {
    Mockito.when(asset.id()).thenReturn(ID_EX);
    Mockito.when(asset.age()).thenReturn(AGE_EX);
    dataRecords = Json.createObjectBuilder().add(ListTypeContract.NAME, NAME_EX).build();
    Mockito.when(asset.data()).thenReturn(dataRecords);
  }

  @Test
  public void invoke_PropertyWasNull_ShouldThrowContractContextException() {
    // Arrange
    JsonObject property = Json.createObjectBuilder().build();
    thrown.expect(ContractContextException.class);
    thrown.expectMessage("property: `" + ListTypeContract.HOLDER_ID + "` is mandatory.");
    // Act
    contract.invoke(ledger, argument, Optional.of(property));
  }

  @Test
  public void invoke_QueryWithNonexistingType_ShouldReturnFailure() {
    // Arrange
    Mockito.when(ledger.scan(Mockito.any(AssetFilter.class))).thenReturn(new ArrayList<Asset>());
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_QuerySingleAssetType_ShouldSucceed() {
    // Arrange
    setAssetRecords();
    historyList.add(asset);
    Mockito.when(ledger.scan(Mockito.any(AssetFilter.class))).thenReturn(historyList);
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Assert.assertEquals(SUCCESS, response.getString(RESULT));
    Assert.assertNotNull(response.get(MESSAGE));
    JsonArray types = response.getJsonArray(ListTypeContract.TYPES);
    JsonObject type = types.getJsonObject(0);
    Assert.assertEquals(NAME_EX, type.getString(ListTypeContract.TYPE));
    Assert.assertEquals(AGE_EX, type.getInt(ListTypeContract.AGE));
  }
}
