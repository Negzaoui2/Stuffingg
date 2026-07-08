import time

from fastapi import APIRouter, HTTPException

from app.models.schemas import (
    OptimizationRequest,
    OptimizationResponse,
    OptimizationWeights,
)
from app.services.data_provider import DatasetProvider, PostgresProvider
from app.services.optimizer import StaffingOptimizer

router = APIRouter()

# Stockage en mémoire du dernier résultat pour /metrics
_last_result: OptimizationResponse | None = None


@router.get("/health")
def health_check():
    return {"status": "healthy", "service": "optimization-engine"}


@router.get("/metrics")
def get_metrics():
    if _last_result is None:
        return {"message": "Aucune optimisation exécutée"}
    return {
        "global_score": _last_result.global_score,
        "metrics": _last_result.metrics,
        "solver_status": _last_result.solver_status,
        "computation_time_ms": _last_result.computation_time_ms,
        "plan_size": len(_last_result.plan),
    }


@router.post("/optimize", response_model=OptimizationResponse)
def optimize(request: OptimizationRequest):
    """Lancer une optimisation d'affectation.

    - source='dataset': utilise le CSV HR enrichi, projets fournis dans la requête
    - source='postgres': utilise les données envoyées par Spring Boot (collaborateurs + projets)
    """
    global _last_result

    if request.source == "dataset":
        provider = DatasetProvider()
        collaborateurs = provider.get_collaborateurs()
        projets = request.projets
        if not projets:
            raise HTTPException(
                status_code=400,
                detail="En mode 'dataset', les projets doivent être fournis dans la requête.",
            )
    elif request.source == "postgres":
        if not request.collaborateurs or not request.projets:
            raise HTTPException(
                status_code=400,
                detail="En mode 'postgres', les collaborateurs et projets doivent être fournis.",
            )
        provider = PostgresProvider(request.collaborateurs, request.projets)
        collaborateurs = provider.get_collaborateurs()
        projets = provider.get_projets()
    else:
        raise HTTPException(status_code=400, detail=f"Source inconnue: {request.source}")

    optimizer = StaffingOptimizer(
        collaborateurs=collaborateurs,
        projets=projets,
        weights=request.weights,
    )
    result = optimizer.solve()
    _last_result = result
    return result


@router.post("/optimize/batch", response_model=list[OptimizationResponse])
def optimize_batch(requests: list[OptimizationRequest]):
    """Optimisation multi-projets en batch."""
    results = []
    for req in requests:
        result = optimize(req)
        results.append(result)
    return results
