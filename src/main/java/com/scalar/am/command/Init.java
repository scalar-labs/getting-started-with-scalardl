package com.scalar.am.command;

import com.scalar.am.contract.AddAssetContract;
import com.scalar.am.contract.AddTypeContract;
import com.scalar.am.contract.AssetHistoryContract;
import com.scalar.am.contract.ListContract;
import com.scalar.am.contract.ListTypeContract;
import com.scalar.am.contract.StatusChangeContract;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.service.ClientService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import picocli.CommandLine;

/** This class defines the behaviour of <em>init</em> CLI command */
@CommandLine.Command(name = "init", description = "Initialize the asset management application")
public class Init extends LedgerClientExecutor implements Runnable {
  @CommandLine.Option(
      names = {"-host"},
      description = "the host address of the scalar DL installation",
      defaultValue = "localhost")
  private String host;

  @CommandLine.Option(
      names = {
        "-p",
      },
      description = "the port of the scalar DL installation",
      defaultValue = "50051")
  private int port;

  @CommandLine.Option(
      names = {"-tls"},
      description = "enable TLS connection")
  private boolean tls;

  @CommandLine.Option(
      names = {"-credential"},
      description = "the authentication cerdential",
      defaultValue = "")
  private String credential;

  @CommandLine.Option(
      names = {"-h"},
      usageHelp = true,
      description = "display this help and exit")
  private boolean help;

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "private-key",
      description = "path to the private key")
  private File privateKey;

  @CommandLine.Parameters(
      index = "1",
      paramLabel = "certificate",
      description = "path to the certificate")
  private File certificate;

  private static final String CONTRACT_DIR =
      System.getProperty("user.dir") + File.separator + "contract";
  private static final String CLIENT_PROPERTIES_PATH =
      System.getProperty("user.dir") + File.separator + "client.properties";

  @Override
  public void run() {
    try {
      ClientConfig clientConfig = createClientConfig();
      LedgerExecutorFunction f =
          (clientService) -> {
            registerCertificate(clientService);
            registerContracts(clientService, clientConfig);
          };
      executeOnLedger(f);
    } catch (Exception e) {

      e.printStackTrace();
    }
  }

  String copyKey() throws IOException {
    Path privateKeyCopy =
        new File(System.getProperty("user.dir") + File.separator + privateKey.getName()).toPath();
    Path privateKey = this.privateKey.toPath();
    Files.copy(privateKey, privateKeyCopy, StandardCopyOption.REPLACE_EXISTING);
    return privateKeyCopy.toAbsolutePath().toString();
  }

  String copyCert() throws IOException {
    Path certificateCopy =
        new File(System.getProperty("user.dir") + File.separator + certificate.getName()).toPath();
    Path certificate = this.certificate.toPath();
    Files.copy(certificate, certificateCopy, StandardCopyOption.REPLACE_EXISTING);
    return certificateCopy.toAbsolutePath().toString();
  }

  private ClientConfig createClientConfig() throws Exception {
    String privateKeyPath = copyKey();
    String certPath = copyCert();

    Properties prop = new Properties();
    prop.setProperty(ClientConfig.SERVER_HOST, this.host);
    prop.setProperty(ClientConfig.SERVER_PORT, Integer.toString(this.port));
    prop.setProperty(ClientConfig.CERT_HOLDER_ID, UUID.randomUUID().toString());
    prop.setProperty(ClientConfig.CERT_VERSION, Integer.toString(1));
    prop.setProperty(ClientConfig.CERT_PATH, certPath);
    prop.setProperty(ClientConfig.PRIVATE_KEY_PATH, privateKeyPath);
    prop.setProperty(ClientConfig.TLS_ENABLED, String.valueOf(this.tls));
    prop.setProperty(ClientConfig.AUTHORIZATION_CREDENTIAL, this.credential);

    OutputStream output = new FileOutputStream(CLIENT_PROPERTIES_PATH);
    prop.store(output, null);
    output.close();

    return new ClientConfig(prop);
  }

  private void registerCertificate(ClientService service) throws Exception {
    try {
      service.registerCertificate();
    }  catch (Exception ex) {
      throw new Exception("Error during certificate registration");
    }
  }

  private void registerContracts(ClientService service, ClientConfig config) throws Exception {
    String holderId = config.getCertHolderId();

    List<Class<?>> contracts =
        Arrays.asList(
            AddAssetContract.class,
            AddTypeContract.class,
            ListTypeContract.class,
            ListContract.class,
            StatusChangeContract.class,
            AssetHistoryContract.class);

    JsonObject property = Json.createObjectBuilder().add("holderId", holderId).build();

    for (Class<?> contract : contracts) {
      File file = new File(CONTRACT_DIR + File.separator + contract.getSimpleName() + ".class");
      if (!file.exists()) {
        throw new Exception("Can not find contract: " + contract.getSimpleName());
      }

      String name = contract.getCanonicalName();
      String id = name + "_" + holderId;
      String path = file.getAbsolutePath();

      try {
        service.registerContract(id, name, path, Optional.ofNullable(property));
      } catch (Exception ex) {
        throw new Exception("Failed to register contract");
      }
    }
  }
}
