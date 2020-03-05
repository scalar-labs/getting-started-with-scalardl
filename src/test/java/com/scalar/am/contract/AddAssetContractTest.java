package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Date;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AddAssetContractTest extends ContractTest {

  private AddAssetContract contract = new AddAssetContract();
  private JsonObject argument;
  private JsonObject property;
  private JsonObject assetStatusJson;
  private JsonObject assetNameJson;

  @Mock private Ledger ledger;
  @Mock private Asset asset;
  @Mock private Asset asset1;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    argument =
        Json.createObjectBuilder()
            .add(AddAssetContract.ID, ID_EX)
            .add(AddAssetContract.TIMESTAMP, new Date().getTime())
            .add(AddAssetContract.TYPE, TYPE_EX)
            .add(AddAssetContract.ASSET, ASSET_EX)
            .build();

    property = Json.createObjectBuilder().add(AddAssetContract.HOLDER_ID, HOLDER_ID_1_EX).build();
  }

  private void setLegerPutValue() {
    assetStatusJson =
        Json.createObjectBuilder()
            .add(AddAssetContract.TIMESTAMP, argument.getJsonNumber(AddAssetContract.TIMESTAMP))
            .add(AddAssetContract.STATUS, IN_STOCK)
            .build();
    assetNameJson =
        Json.createObjectBuilder()
            .add(AddAssetContract.ID, argument.getString(AddAssetContract.ID))
            .add(AddAssetContract.NAME, argument.getString(AddAssetContract.ASSET))
            .build();
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
    thrown.expectMessage("property: `" + AddAssetContract.HOLDER_ID + "` is mandatory.");
    // Act
    contract.invoke(ledger, argument, Optional.of(property));
  }

  @Test
  public void invoke_AddNonExistingAsset_ShouldReturnFailure() {
    // Arrange
    Mockito.when(ledger.get(TYPE_EX)).thenReturn(Optional.empty());
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.never()).put(ID_EX, assetStatusJson);
    Mockito.verify(ledger, Mockito.never()).put(TYPE_EX, assetStatusJson);
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_AddAlreadyRegisteredAsset_ShouldReturnFailure() {
    // Arrange
    Mockito.when(ledger.get(TYPE_EX)).thenReturn(Optional.of(asset));
    Mockito.when(ledger.get(ID_EX)).thenReturn(Optional.of(asset1));
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.never()).put(ID_EX, assetStatusJson);
    Mockito.verify(ledger, Mockito.never()).put(TYPE_EX, assetStatusJson);
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_AddRegisteredAsset_ShouldRunProperly() {
    // Arrange
    setLegerPutValue();
    Mockito.when(ledger.get(HOLDER_ID_1_EX + "-" + TYPE_EX)).thenReturn(Optional.of(asset));
    Mockito.when(ledger.get(HOLDER_ID_1_EX + "-" + ID_EX)).thenReturn(Optional.empty());
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.times(1)).put(HOLDER_ID_1_EX + "-" + ID_EX, assetStatusJson);
    Mockito.verify(ledger, Mockito.times(1)).put(HOLDER_ID_1_EX + "-" + TYPE_EX, assetNameJson);
    Assert.assertEquals(SUCCESS, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }
}
