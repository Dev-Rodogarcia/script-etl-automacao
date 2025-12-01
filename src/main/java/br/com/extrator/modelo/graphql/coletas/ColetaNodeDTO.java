package br.com.extrator.modelo.graphql.coletas;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um "node" de Coleta,
 * conforme retornado pela API GraphQL.
 * Mapeia todos os campos válidos do tipo Pick e inclui um contêiner
 * dinâmico para garantir a captura de quaisquer outros campos que a query
 * possa vir a retornar no futuro.
 */
public class ColetaNodeDTO {

    // --- Campos Essenciais do Tipo Pick ---
    @JsonProperty("id")
    private String id;

    @JsonProperty("agentId")
    private Long agentId;

    @JsonProperty("cancellationReason")
    private String cancellationReason;

    @JsonProperty("cancellationUserId")
    private Long cancellationUserId;

    @JsonProperty("cargoClassificationId")
    private Long cargoClassificationId;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("costCenterId")
    private Long costCenterId;

    @JsonProperty("destroyReason")
    private String destroyReason;

    @JsonProperty("destroyUserId")
    private Long destroyUserId;

    @JsonProperty("invoicesCubedWeight")
    private BigDecimal invoicesCubedWeight;

    @JsonProperty("invoicesValue")
    private BigDecimal invoicesValue;

    @JsonProperty("invoicesVolumes")
    private Integer invoicesVolumes;

    @JsonProperty("invoicesWeight")
    private BigDecimal invoicesWeight;

    @JsonProperty("lunchBreakEndHour")
    private String lunchBreakEndHour;

    @JsonProperty("lunchBreakStartHour")
    private String lunchBreakStartHour;

    @JsonProperty("notificationEmail")
    private String notificationEmail;

    @JsonProperty("notificationPhone")
    private String notificationPhone;

    @JsonProperty("pickTypeId")
    private Long pickTypeId;

    @JsonProperty("pickupLocationId")
    private Long pickupLocationId;

    @JsonProperty("requestDate")
    private String requestDate; // Recebido como String YYYY-MM-DD

    @JsonProperty("requestHour")
    private String requestHour;

    @JsonProperty("requester")
    private String requester;

    @JsonProperty("sequenceCode")
    private Long sequenceCode;

    @JsonProperty("serviceDate")
    private String serviceDate; // Recebido como String YYYY-MM-DD

    @JsonProperty("serviceEndHour")
    private String serviceEndHour;

    @JsonProperty("serviceStartHour")
    private String serviceStartHour;

    @JsonProperty("status")
    private String status;

    @JsonProperty("statusUpdatedAt")
    private String statusUpdatedAt;

    @JsonProperty("taxedWeight")
    private BigDecimal taxedWeight;

    @JsonProperty("vehicleTypeId")
    private Long vehicleTypeId;

    @JsonProperty("manifestItemPickId")
    private Long manifestItemPickId;

    // --- Campos Expandidos (Objetos Aninhados) ---
    @JsonProperty("customer")
    private CustomerDTO customer;

    @JsonProperty("pickAddress")
    private PickAddressDTO pickAddress;

    @JsonProperty("user")
    private UserDTO user;

    @JsonProperty("corporation")
    private Corporation corporation;

    // --- Campos Adicionais do CSV (22 campos mapeados) ---
    @JsonProperty("finishDate")
    private String finishDate; // Recebido como String YYYY-MM-DD

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    // --- Getters e Setters ---

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(final Long agentId) {
        this.agentId = agentId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(final String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Long getCancellationUserId() {
        return cancellationUserId;
    }

    public void setCancellationUserId(final Long cancellationUserId) {
        this.cancellationUserId = cancellationUserId;
    }

    public Long getCargoClassificationId() {
        return cargoClassificationId;
    }

    public void setCargoClassificationId(final Long cargoClassificationId) {
        this.cargoClassificationId = cargoClassificationId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(final String comments) {
        this.comments = comments;
    }

    public Long getCostCenterId() {
        return costCenterId;
    }

    public void setCostCenterId(final Long costCenterId) {
        this.costCenterId = costCenterId;
    }

    public String getDestroyReason() {
        return destroyReason;
    }

    public void setDestroyReason(final String destroyReason) {
        this.destroyReason = destroyReason;
    }

    public Long getDestroyUserId() {
        return destroyUserId;
    }

    public void setDestroyUserId(final Long destroyUserId) {
        this.destroyUserId = destroyUserId;
    }

    public BigDecimal getInvoicesCubedWeight() {
        return invoicesCubedWeight;
    }

    public void setInvoicesCubedWeight(final BigDecimal invoicesCubedWeight) {
        this.invoicesCubedWeight = invoicesCubedWeight;
    }

    public BigDecimal getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final BigDecimal invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public Integer getInvoicesVolumes() {
        return invoicesVolumes;
    }

    public void setInvoicesVolumes(final Integer invoicesVolumes) {
        this.invoicesVolumes = invoicesVolumes;
    }

    public BigDecimal getInvoicesWeight() {
        return invoicesWeight;
    }

    public void setInvoicesWeight(final BigDecimal invoicesWeight) {
        this.invoicesWeight = invoicesWeight;
    }

    public String getLunchBreakEndHour() {
        return lunchBreakEndHour;
    }

    public void setLunchBreakEndHour(final String lunchBreakEndHour) {
        this.lunchBreakEndHour = lunchBreakEndHour;
    }

    public String getLunchBreakStartHour() {
        return lunchBreakStartHour;
    }

    public void setLunchBreakStartHour(final String lunchBreakStartHour) {
        this.lunchBreakStartHour = lunchBreakStartHour;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(final String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public String getNotificationPhone() {
        return notificationPhone;
    }

    public void setNotificationPhone(final String notificationPhone) {
        this.notificationPhone = notificationPhone;
    }

    public Long getPickTypeId() {
        return pickTypeId;
    }

    public void setPickTypeId(final Long pickTypeId) {
        this.pickTypeId = pickTypeId;
    }

    public Long getPickupLocationId() {
        return pickupLocationId;
    }

    public void setPickupLocationId(final Long pickupLocationId) {
        this.pickupLocationId = pickupLocationId;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(final String requestDate) {
        this.requestDate = requestDate;
    }

    public String getRequestHour() {
        return requestHour;
    }

    public void setRequestHour(final String requestHour) {
        this.requestHour = requestHour;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(final String requester) {
        this.requester = requester;
    }

    public Long getSequenceCode() {
        return sequenceCode;
    }

    public void setSequenceCode(final Long sequenceCode) {
        this.sequenceCode = sequenceCode;
    }

    public String getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(final String serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getServiceEndHour() {
        return serviceEndHour;
    }

    public void setServiceEndHour(final String serviceEndHour) {
        this.serviceEndHour = serviceEndHour;
    }

    public String getServiceStartHour() {
        return serviceStartHour;
    }

    public void setServiceStartHour(final String serviceStartHour) {
        this.serviceStartHour = serviceStartHour;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(final String statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(final BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public Long getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(final Long vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
    }

    public Long getManifestItemPickId() {
        return manifestItemPickId;
    }

    public void setManifestItemPickId(Long manifestItemPickId) {
        this.manifestItemPickId = manifestItemPickId;
    }

    // --- Getters e Setters para Campos Expandidos ---

    public CustomerDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDTO customer) {
        this.customer = customer;
    }

    public PickAddressDTO getPickAddress() {
        return pickAddress;
    }

    public void setPickAddress(PickAddressDTO pickAddress) {
        this.pickAddress = pickAddress;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public Corporation getCorporation() {
        return corporation;
    }

    public void setCorporation(Corporation corporation) {
        this.corporation = corporation;
    }

    // --- Getters e Setters para Campos Adicionais do CSV ---

    public String getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(String finishDate) {
        this.finishDate = finishDate;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    public static class Corporation {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("person")
        private Person person;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Person getPerson() { return person; }
        public void setPerson(Person person) { this.person = person; }
    }

    public static class Person {
        @JsonProperty("nickname")
        private String nickname;

        @JsonProperty("cnpj")
        private String cnpj;

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getCnpj() { return cnpj; }
        public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    }
}
