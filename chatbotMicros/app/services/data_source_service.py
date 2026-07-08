from __future__ import annotations

from pathlib import Path

import pandas as pd

from app.config.settings import get_settings


def load_hr_dataframe() -> pd.DataFrame | None:
	"""Loads HR data from the configured source (dataset or PostgreSQL)."""
	settings = get_settings()

	if settings.data_source == "dataset":
		if not settings.dataset_path:
			print("[DATA] DATA_SOURCE=dataset mais DATASET_PATH est vide.")
			return None
		dataset_path = Path(settings.dataset_path)
		if not dataset_path.exists():
			print(f"[DATA] Dataset introuvable: {dataset_path}")
			return None
		print(f"[DATA] Chargement depuis dataset: {dataset_path}")
		return pd.read_csv(dataset_path)

	if settings.data_source == "postgres":
		if not settings.postgres_dsn:
			print("[DATA] DATA_SOURCE=postgres mais POSTGRES_DSN est vide.")
			return None
		query = settings.postgres_query or f"SELECT * FROM {settings.postgres_table}"
		source = "requete personnalisee" if settings.postgres_query else f"table={settings.postgres_table}"
		print(f"[DATA] Chargement depuis PostgreSQL: {source}")
		return pd.read_sql(query, settings.postgres_dsn)

	print(f"[DATA] DATA_SOURCE invalide: {settings.data_source}")
	return None
