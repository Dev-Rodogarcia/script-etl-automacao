package br.com.extrator.modelo.graphql.fretes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO (Data Transfer Object) para representar um "node" de Frete,
 * conforme retornado pela API GraphQL. Mapeia os campos essenciais
 * e inclui um contêiner dinâmico para capturar todas as outras
 * propriedades, garantindo resiliência e completude.
 */
public class FreteNodeDTO {

    // --- Campos Essenciais Mapeados ---
    @JsonProperty("id")
    private Long id;

    @JsonProperty("serviceAt")
    private String serviceAt; // Recebe como String para ser convertido para OffsetDateTime

    @JsonProperty("createdAt")
    private String createdAt; // Recebe como String para ser convertido para OffsetDateTime

    @JsonProperty("status")
    private String status;

    @JsonProperty("modal")
    private String modal;

    @JsonProperty("type")
    private String type;

    @JsonProperty("total")
    private BigDecimal totalValue;

    @JsonProperty("invoicesValue")
    private BigDecimal invoicesValue;

    @JsonProperty("invoicesWeight")
    private BigDecimal invoicesWeight;

    @JsonProperty("corporationId")
    private Long corporationId;

    @JsonProperty("destinationCityId")
    private Long destinationCityId;

    @JsonProperty("deliveryPredictionDate")
    private String deliveryPredictionDate; // Recebe como String para ser convertido para LocalDate

    // --- Campos Expandidos (Objetos Aninhados) ---
    @JsonProperty("payer")
    private PayerDTO payer;

    @JsonProperty("sender")
    private SenderDTO sender;

    @JsonProperty("receiver")
    private ReceiverDTO receiver;

    @JsonProperty("corporation")
    private CorporationDTO corporation;

    @JsonProperty("freightInvoices")
    private List<FreightInvoiceDTO> freightInvoices;

    @JsonProperty("customerPriceTable")
    private CustomerPriceTableDTO customerPriceTable;

    @JsonProperty("freightClassification")
    private FreightClassificationDTO freightClassification;

    @JsonProperty("costCenter")
    private CostCenterDTO costCenter;

    @JsonProperty("user")
    private UserDTO user;

    // --- Campos Adicionais do CSV (22 campos mapeados) ---
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @JsonProperty("invoicesTotalVolumes")
    private Integer invoicesTotalVolumes;

    @JsonProperty("taxedWeight")
    private BigDecimal taxedWeight;

    @JsonProperty("realWeight")
    private BigDecimal realWeight;

    @JsonProperty("totalCubicVolume")
    private BigDecimal totalCubicVolume;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    // --- Contêiner Dinâmico ("Resto") ---
    private final Map<String, Object> otherProperties = new HashMap<>();

    @JsonAnySetter
    public void add(final String key, final Object value) {
        this.otherProperties.put(key, value);
    }

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getServiceAt() {
        return serviceAt;
    }

    public void setServiceAt(final String serviceAt) {
        this.serviceAt = serviceAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(final String modal) {
        this.modal = modal;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(final BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getInvoicesValue() {
        return invoicesValue;
    }

    public void setInvoicesValue(final BigDecimal invoicesValue) {
        this.invoicesValue = invoicesValue;
    }

    public BigDecimal getInvoicesWeight() {
        return invoicesWeight;
    }

    public void setInvoicesWeight(final BigDecimal invoicesWeight) {
        this.invoicesWeight = invoicesWeight;
    }

    public Long getCorporationId() {
        return corporationId;
    }

    public void setCorporationId(final Long corporationId) {
        this.corporationId = corporationId;
    }

    public Long getDestinationCityId() {
        return destinationCityId;
    }

    public void setDestinationCityId(final Long destinationCityId) {
        this.destinationCityId = destinationCityId;
    }

    public String getDeliveryPredictionDate() {
        return deliveryPredictionDate;
    }

    public void setDeliveryPredictionDate(final String deliveryPredictionDate) {
        this.deliveryPredictionDate = deliveryPredictionDate;
    }

    // --- Getters e Setters para Campos Expandidos ---

    public PayerDTO getPayer() {
        return payer;
    }

    public void setPayer(PayerDTO payer) {
        this.payer = payer;
    }

    public SenderDTO getSender() {
        return sender;
    }

    public void setSender(SenderDTO sender) {
        this.sender = sender;
    }

    public ReceiverDTO getReceiver() {
        return receiver;
    }

    public void setReceiver(ReceiverDTO receiver) {
        this.receiver = receiver;
    }

    public CorporationDTO getCorporation() {
        return corporation;
    }

    public void setCorporation(CorporationDTO corporation) {
        this.corporation = corporation;
    }

    public List<FreightInvoiceDTO> getFreightInvoices() {
        return freightInvoices;
    }

    public void setFreightInvoices(List<FreightInvoiceDTO> freightInvoices) {
        this.freightInvoices = freightInvoices;
    }

    public CustomerPriceTableDTO getCustomerPriceTable() {
        return customerPriceTable;
    }

    public void setCustomerPriceTable(CustomerPriceTableDTO customerPriceTable) {
        this.customerPriceTable = customerPriceTable;
    }

    public FreightClassificationDTO getFreightClassification() {
        return freightClassification;
    }

    public void setFreightClassification(FreightClassificationDTO freightClassification) {
        this.freightClassification = freightClassification;
    }

    public CostCenterDTO getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(CostCenterDTO costCenter) {
        this.costCenter = costCenter;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    // --- Getters e Setters para Campos Adicionais do CSV ---

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Integer getInvoicesTotalVolumes() {
        return invoicesTotalVolumes;
    }

    public void setInvoicesTotalVolumes(Integer invoicesTotalVolumes) {
        this.invoicesTotalVolumes = invoicesTotalVolumes;
    }

    public BigDecimal getTaxedWeight() {
        return taxedWeight;
    }

    public void setTaxedWeight(BigDecimal taxedWeight) {
        this.taxedWeight = taxedWeight;
    }

    public BigDecimal getRealWeight() {
        return realWeight;
    }

    public void setRealWeight(BigDecimal realWeight) {
        this.realWeight = realWeight;
    }

    public BigDecimal getTotalCubicVolume() {
        return totalCubicVolume;
    }

    public void setTotalCubicVolume(BigDecimal totalCubicVolume) {
        this.totalCubicVolume = totalCubicVolume;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }
}
