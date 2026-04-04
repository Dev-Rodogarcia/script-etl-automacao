IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'dbo.sinistros') AND type in (N'U'))
BEGIN
    CREATE TABLE dbo.sinistros (
        identificador_unico NVARCHAR(64) PRIMARY KEY,
        sequence_code BIGINT NOT NULL,
        opening_at_date DATE NULL,
        occurrence_at_date DATE NULL,
        occurrence_at_time NVARCHAR(20) NULL,
        expected_solution_date DATE NULL,
        insurance_claim_location NVARCHAR(255) NULL,
        informed_by NVARCHAR(255) NULL,
        finished_at_date DATE NULL,
        finished_at_time NVARCHAR(20) NULL,
        invoices_count INT NULL,
        corporation_sequence_number BIGINT NULL,
        insurance_occurrence_number BIGINT NULL,
        invoices_volumes INT NULL,
        invoices_weight DECIMAL(18, 3) NULL,
        invoices_value DECIMAL(18, 2) NULL,
        payer_nickname NVARCHAR(255) NULL,
        customer_debits_subtotal DECIMAL(18, 2) NULL,
        customer_credit_entries_subtotal DECIMAL(18, 2) NULL,
        responsible_credits_subtotal DECIMAL(18, 2) NULL,
        responsible_debit_entries_subtotal DECIMAL(18, 2) NULL,
        insurer_credits_subtotal DECIMAL(18, 2) NULL,
        insurance_claim_total DECIMAL(18, 2) NULL,
        branch_nickname NVARCHAR(255) NULL,
        event_name NVARCHAR(255) NULL,
        user_name NVARCHAR(255) NULL,
        vehicle_plate NVARCHAR(20) NULL,
        occurrence_description NVARCHAR(500) NULL,
        occurrence_code NVARCHAR(100) NULL,
        treatment_at DATETIMEOFFSET NULL,
        dealing_type NVARCHAR(100) NULL,
        solution_type NVARCHAR(100) NULL,
        metadata NVARCHAR(MAX) NULL,
        data_extracao DATETIME2 DEFAULT GETDATE()
    );

    CREATE INDEX IX_sinistros_sequence_code ON dbo.sinistros (sequence_code);
    CREATE INDEX IX_sinistros_minuta ON dbo.sinistros (corporation_sequence_number);
END
GO
