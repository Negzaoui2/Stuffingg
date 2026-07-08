from pydantic import BaseModel, Field


class Collaborateur(BaseModel):
    id: str
    name: str
    department: str | None = None
    job_role: str | None = None
    main_domain: str | None = None
    skills: list[str] = Field(default_factory=list)
    seniority: str | None = None
    certifications: list[str] = Field(default_factory=list)
    daily_cost: float = 0.0
    available_days: int = 0
    absence_days_last_12_months: int = 0
    main_absence_type: str | None = None
    current_fte: float = 0.0


class Projet(BaseModel):
    id: str
    name: str
    required_skills: list[str] = Field(default_factory=list)
    required_seniority: str | None = None
    start_date: str | None = None
    end_date: str | None = None
    priority: int = Field(default=1, ge=1, le=5)
    headcount_needed: int = 1


class OptimizationRequest(BaseModel):
    """Requête d'optimisation - peut contenir les données directement (mode Postgres)
    ou utiliser le dataset (mode chatbot)."""

    source: str = Field(
        default="dataset",
        description="Source des données: 'dataset' (CSV) ou 'postgres' (données envoyées par Spring Boot)",
    )
    projets: list[Projet] = Field(default_factory=list)
    collaborateurs: list[Collaborateur] = Field(default_factory=list)
    weights: "OptimizationWeights | None" = None


class OptimizationWeights(BaseModel):
    skills_match: float = Field(default=0.4, ge=0.0, le=1.0)
    cost: float = Field(default=0.3, ge=0.0, le=1.0)
    availability: float = Field(default=0.2, ge=0.0, le=1.0)
    balance: float = Field(default=0.1, ge=0.0, le=1.0)


class Affectation(BaseModel):
    collaborateur_id: str
    collaborateur_name: str
    projet_id: str
    projet_name: str
    allocation_percentage: int
    skills_match_score: float
    period: str | None = None


class OptimizationResponse(BaseModel):
    plan: list[Affectation]
    global_score: float
    metrics: dict[str, float]
    solver_status: str
    computation_time_ms: float
