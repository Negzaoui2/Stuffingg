from abc import ABC, abstractmethod

import pandas as pd

from app.config import settings
from app.models.schemas import Collaborateur, Projet


class DataProvider(ABC):
    """Abstraction pour la source de données du moteur."""

    @abstractmethod
    def get_collaborateurs(self) -> list[Collaborateur]:
        pass

    @abstractmethod
    def get_projets(self) -> list[Projet]:
        pass


class DatasetProvider(DataProvider):
    """Fournit les données depuis le dataset HR enrichi (CSV) — mode Chatbot."""

    def __init__(self, dataset_path: str | None = None):
        self.dataset_path = dataset_path or settings.dataset_path
        self._df: pd.DataFrame | None = None

    def _load(self) -> pd.DataFrame:
        if self._df is None:
            self._df = pd.read_csv(self.dataset_path)
        return self._df

    def get_collaborateurs(self) -> list[Collaborateur]:
        df = self._load()
        collaborateurs = []
        for _, row in df.iterrows():
            skills = (
                [s.strip() for s in str(row.get("Skills", "")).split(",")]
                if pd.notna(row.get("Skills"))
                else []
            )
            certifications = (
                [c.strip() for c in str(row.get("Certifications", "")).split(",")]
                if pd.notna(row.get("Certifications"))
                else []
            )
            seniority = str(row.get("Seniority", "Junior"))
            cost_map = {"Junior": 300, "Mid": 500, "Senior": 800, "Lead": 1000}
            daily_cost = cost_map.get(seniority, 400)

            collaborateurs.append(
                Collaborateur(
                    id=str(row.get("EmployeeId", row.name)),
                    name=str(row.get("FullName", f"Collaborateur_{row.name}")),
                    department=str(row.get("Department", "")) if pd.notna(row.get("Department")) else None,
                    job_role=str(row.get("JobRole", "")) if pd.notna(row.get("JobRole")) else None,
                    main_domain=str(row.get("MainDomain", "")) if pd.notna(row.get("MainDomain")) else None,
                    skills=skills,
                    seniority=seniority,
                    certifications=certifications,
                    daily_cost=daily_cost,
                    available_days=max(0, 220 - int(row.get("AbsenceDaysLast12Months", 0))),
                    absence_days_last_12_months=int(row.get("AbsenceDaysLast12Months", 0)),
                    main_absence_type=str(row.get("MainAbsenceType", "")) if pd.notna(row.get("MainAbsenceType")) else None,
                    current_fte=0.0,
                )
            )
        return collaborateurs

    def get_projets(self) -> list[Projet]:
        # En mode dataset, les projets sont fournis dans la requête
        return []


class PostgresProvider(DataProvider):
    """Fournit les données directement depuis la requête (envoyées par Spring Boot)."""

    def __init__(self, collaborateurs: list[Collaborateur], projets: list[Projet]):
        self._collaborateurs = collaborateurs
        self._projets = projets

    def get_collaborateurs(self) -> list[Collaborateur]:
        return self._collaborateurs

    def get_projets(self) -> list[Projet]:
        return self._projets
