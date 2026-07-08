from fastapi import FastAPI
from app.api.routes import router
from app.config import settings

app = FastAPI(
    title="Moteur d'Optimisation - Stuffing",
    description="Service d'optimisation d'affectation collaborateurs/projets (OR-Tools CP-SAT)",
    version="1.0.0",
)

app.include_router(router, prefix="/api/v1")


@app.get("/")
def root():
    return {"service": "optimization-engine", "status": "running"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.app_port, reload=True)
