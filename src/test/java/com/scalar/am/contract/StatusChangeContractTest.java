package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
import java.util.Date;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class StatusChangeContractTest extends ContractTest {

  private StatusChangeContract contract = new StatusChangeContract();
  private JsonObjectBuilder argumentBuilder = Json.createObjectBuilder();
  private JsonObjectBuilder assetStatusJsonBuilder = Json.createObjectBuilder();
  private JsonObjectBuilder propertyBuilder = Json.createObjectBuilder();

  @Mock private Ledger ledger;
  @Mock private Asset asset;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    argumentBuilder
        .add(StatusChangeContract.ASSET_ID, ASSET_ID_EX)
        .add(StatusChangeContract.TIMESTAMP, new Date().getTime());

    Mockito.when(asset.id()).thenReturn(ID_EX);
    Mockito.when(asset.age()).thenReturn(AGE_EX);
  }

  private void setNewStatusAndCurrentStatus(String newStatus, String currentStatus) {
    argumentBuilder.add(StatusChangeContract.STATUS, newStatus);
    if (currentStatus != null) {
      assetStatusJsonBuilder.add(StatusChangeContract.STATUS, currentStatus);
    }
  }

  private void setHasContractHolderIdAndHasAssetHolderId(
      String contractHolderId, String assetHolderId) {
    propertyBuilder.add(StatusChangeContract.HOLDER_ID, contractHolderId);
    assetStatusJsonBuilder.add(StatusChangeContract.HOLDER_ID, assetHolderId);
  }

  @Test
  public void invoke_ArgumentWasNull_ShouldThrowContractContextException() {
    // Arrange
    JsonObject argument = Json.createObjectBuilder().build();
    thrown.expect(ContractContextException.class);
    thrown.expectMessage("wrong argument.");
    // Act
    contract.invoke(ledger, argument, Optional.empty());
    // Assert
    Mockito.verify(ledger, Mockito.never()).put(ASSET_ID_EX, assetStatusJsonBuilder.build());
  }

  @Test
  public void invoke_PropertyWasNull_ShouldThrowContractContextException() {
    // Arrange
    JsonObject property = Json.createObjectBuilder().build();
    JsonObject argument =
        argumentBuilder.add(StatusChangeContract.STATUS, StatusChangeContract.ON_LOAN).build();
    thrown.expect(ContractContextException.class);
    thrown.expectMessage("property: `" + StatusChangeContract.HOLDER_ID + "` is mandatory.");
    // Act
    contract.invoke(ledger, argument, Optional.of(property));
  }

  @Test
  public void invoke_QueryNonexistingAsset_ShouldReturnFailure() {
    // Arrange
    Mockito.when(ledger.get(HOLDER_ID_1_EX + "-" + ASSET_ID_EX)).thenReturn(Optional.empty());
    JsonObject argument =
        Json.createObjectBuilder()
            .add(StatusChangeContract.ASSET_ID, ASSET_ID_EX)
            .add(StatusChangeContract.TIMESTAMP, 0) // N/A
            .add(StatusChangeContract.STATUS, "") // N/A
            .build();
    JsonObject property =
        propertyBuilder.add(StatusChangeContract.HOLDER_ID, HOLDER_ID_1_EX).build();
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.never()).put(ASSET_ID_EX, assetStatusJsonBuilder.build());
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_BorrowAlreadyBorrowedAsset_ShouldReturnFailure() {
    // Arrange
    setNewStatusAndCurrentStatus(StatusChangeContract.ON_LOAN, StatusChangeContract.ON_LOAN);
    Mockito.when(asset.data()).thenReturn(assetStatusJsonBuilder.build());
    Mockito.when(ledger.get(ASSET_ID_EX)).thenReturn(Optional.of(asset));
    JsonObject property =
        propertyBuilder.add(StatusChangeContract.HOLDER_ID, HOLDER_ID_1_EX).build();
    // Act
    JsonObject response = contract.invoke(ledger, argumentBuilder.build(), Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.never()).put(ASSET_ID_EX, assetStatusJsonBuilder.build());
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_ReturnAlreadyInStockAsset_ShouldReturnFailure() {
    // Arrange
    setNewStatusAndCurrentStatus(StatusChangeContract.IN_STOCK, StatusChangeContract.IN_STOCK);
    Mockito.when(asset.data()).thenReturn(assetStatusJsonBuilder.build());
    Mockito.when(ledger.get(ASSET_ID_EX)).thenReturn(Optional.of(asset));
    JsonObject property =
        propertyBuilder.add(StatusChangeContract.HOLDER_ID, HOLDER_ID_1_EX).build();
    // Act
    JsonObject response = contract.invoke(ledger, argumentBuilder.build(), Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.never()).put(ASSET_ID_EX, assetStatusJsonBuilder.build());
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_BorrowedAssetanOtherUser_ShouldReturnFailure() {
    // Arrange
    setNewStatusAndCurrentStatus(StatusChangeContract.IN_STOCK, StatusChangeContract.ON_LOAN);
    setHasContractHolderIdAndHasAssetHolderId(HOLDER_ID_1_EX, HOLDER_ID_2_EX);
    Mockito.when(asset.data()).thenReturn(assetStatusJsonBuilder.build());
    Mockito.when(ledger.get(ASSET_ID_EX)).thenReturn(Optional.of(asset));
    // Act
    JsonObject response =
        contract.invoke(ledger, argumentBuilder.build(), Optional.of(propertyBuilder.build()));
    // Assert
    Mockito.verify(ledger, Mockito.never()).put(ASSET_ID_EX, assetStatusJsonBuilder.build());
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_ReturnOnLoanAsset_ShouldRunProperly() {
    // Arrange
    JsonObject currentStatus =
        Json.createObjectBuilder()
            .add(StatusChangeContract.STATUS, StatusChangeContract.ON_LOAN)
            .add(StatusChangeContract.HOLDER_ID, HOLDER_ID_1_EX)
            .add(StatusChangeContract.TIMESTAMP, new Date().getTime())
            .build();
    Mockito.when(asset.data()).thenReturn(currentStatus);
    Mockito.when(ledger.get(HOLDER_ID_1_EX + "-" + ASSET_ID_EX)).thenReturn(Optional.of(asset));
    long timestamp = new Date().getTime();
    JsonObject argument =
        Json.createObjectBuilder()
            .add(StatusChangeContract.TIMESTAMP, timestamp)
            .add(StatusChangeContract.STATUS, StatusChangeContract.IN_STOCK)
            .add(StatusChangeContract.ASSET_ID, ASSET_ID_EX)
            .build();
    JsonObject property =
        Json.createObjectBuilder().add(StatusChangeContract.HOLDER_ID, HOLDER_ID_1_EX).build();
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    JsonObject assertObject =
        Json.createObjectBuilder()
            .add(StatusChangeContract.TIMESTAMP, timestamp)
            .add(StatusChangeContract.STATUS, StatusChangeContract.IN_STOCK)
            .build();
    Mockito.verify(ledger, Mockito.times(1)).put(HOLDER_ID_1_EX + "-" + ASSET_ID_EX, assertObject);
    Assert.assertEquals(SUCCESS, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }
}
