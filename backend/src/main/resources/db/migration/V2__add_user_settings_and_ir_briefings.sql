CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    kimi_api_key_encrypted TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ir_briefings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_entry_id UUID NOT NULL REFERENCES user_portfolios(id) ON DELETE CASCADE,
    briefing_json TEXT NOT NULL,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ir_briefings_portfolio_entry_id ON ir_briefings(portfolio_entry_id);
