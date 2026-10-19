CREATE TABLE IF NOT EXISTS shipment_xml_data (
    id SERIAL PRIMARY KEY,
    xml_file TEXT,
    error_message TEXT
);
