package com.scalar.am.contract;

import com.scalar.dl.ledger.asset.Asset;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.database.Ledger;
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

public class AddTypeContractTest extends ContractTest {

  private AddTypeContract contract = new AddTypeContract();
  private JsonObject argument;
  private JsonObject property;
  private JsonObject newType;

  @Mock private Ledger ledger;
  @Mock private Asset asset;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    argument = Json.createObjectBuilder().add(AddTypeContract.NAME, NAME_EX).build();
    property = Json.createObjectBuilder().add(AddAssetContract.HOLDER_ID, HOLDER_ID_1_EX).build();
  }

  private void setAssetRecords() {
    Mockito.when(asset.id()).thenReturn(ID_EX);
    Mockito.when(asset.age()).thenReturn(AGE_EX);
    Mockito.when(asset.data()).thenReturn(null);
  }

  private void setNewType() {
    newType =
        Json.createObjectBuilder()
            .add(AddTypeContract.NAME, argument.getString(AddTypeContract.NAME))
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
    thrown.expectMessage("property: `" + AddTypeContract.HOLDER_ID + "` is mandatory.");
    // Act
    contract.invoke(ledger, argument, Optional.of(property));
  }

  @Test
  public void invoke_TypeAlreadyRegistered_ShouldReturnFailure() {
    // Arrange
    setAssetRecords();
    Mockito.when(ledger.get(HOLDER_ID_1_EX + "-" + NAME_EX)).thenReturn(Optional.of(asset));
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.never())
        .put(HOLDER_ID_1_EX + "-" + AddTypeContract.TYPE, newType);
    Assert.assertEquals(FAILURE, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }

  @Test
  public void invoke_InsertNewType_ShouldRunProperly() {
    // Arrange
    setNewType();
    Mockito.when(ledger.get(HOLDER_ID_1_EX + "-" + NAME_EX)).thenReturn(Optional.empty());
    // Act
    JsonObject response = contract.invoke(ledger, argument, Optional.of(property));
    // Assert
    Mockito.verify(ledger, Mockito.times(1))
        .put(HOLDER_ID_1_EX + "-" + AddTypeContract.TYPE, newType);
    Assert.assertEquals(SUCCESS, response.getString(RESULT));
    Assert.assertNotNull(response.getString(MESSAGE));
  }
}
