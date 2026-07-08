from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_env: str = "development"
    app_port: int = 8001
    dataset_path: str = "data/hr_dataset.csv"
    spring_boot_url: str = "http://localhost:808"

    class Config:
        env_file = ".env"


settings = Settings()
