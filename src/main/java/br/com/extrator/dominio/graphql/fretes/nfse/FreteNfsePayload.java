package br.com.extrator.dominio.graphql.fretes.nfse;

public interface FreteNfsePayload {
    String getId();
    Integer getNumber();
    String getStatus();
    String getRpsSeries();
    String getIssuedAt();
    String getCancelationReason();
    String getPdfServiceUrl();
    String getXmlDocument();
    Long getCorporationId();
    Long getFreightId();
    String getServiceDescription();
}
