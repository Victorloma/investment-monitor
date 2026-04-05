ALTER TABLE user_settings
    RENAME COLUMN kimi_api_key_encrypted TO llm_api_key_encrypted;

ALTER TABLE user_settings
    ADD COLUMN llm_provider_url VARCHAR(500),
    ADD COLUMN llm_model_id     VARCHAR(200);

UPDATE user_settings
SET llm_provider_url = 'https://integrate.api.nvidia.com/v1',
    llm_model_id     = 'moonshotai/kimi-k2-5'
WHERE llm_provider_url IS NULL;
