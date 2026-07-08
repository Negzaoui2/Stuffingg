# Chatbot Microservice (Python)

Microservice FastAPI pour un assistant RH intelligent (PFE – Plateforme de planification de staffing) avec **RAG** basé sur le dataset **HR-Employee-Attrition** (Kaggle, format `.xlsb`).

## Dataset

- **Nom** : HR-Employee-Attrition.xlsb
- **Colonnes** : Age, Attrition, BusinessTravel, DailyRate, Departement, DistanceFromHome, Education, EducationField, EmployeeCount, EmployeeNumber, EnvironnmentStatisfaction, Gender, HourlyRate, JobInvolvement, JobLevel, JobRole, JobSatisfaction, MaritalStatus, MonthlyIncome, MonthlyRate, NumCompaniesWorked, Over18, OverTime, PercentSalaryHike, PerformanceRating, RelationShipSatisfaction, StandardHours, StockOptionLevel, TotalWorkingYears, TrainingTimesLastYear, WorkLifeBalance, YearsAtCompany, YearsInCurrentRole, YearsSinceLastPromotion, YearsWithCurrManager

## Démarrage

1) Installer les dépendances

```bash
pip install -r requirements.txt
```

2) Configurer `.env` (déjà créé, à adapter)

Mode 1 : source dataset (CSV)

```env
DATA_SOURCE=dataset
DATASET_PATH=C:\chemin\vers\HR-Employee-Attrition.csv
DATASET_TEXT_COLUMNS=Attrition,BusinessTravel,Departement,EducationField,Gender,JobRole,MaritalStatus,OverTime
```

Mode 2 : source PostgreSQL

```env
DATA_SOURCE=postgres
POSTGRES_DSN=postgresql+psycopg://username:password@localhost:5432/Stuffing
POSTGRES_TABLE=hr_employee_attrition
DATASET_TEXT_COLUMNS=Attrition,BusinessTravel,Department,EducationField,Gender,JobRole,MaritalStatus,OverTime
```

Option avancee (recommandee si vos donnees sont reparties sur plusieurs tables) :

```env
DATA_SOURCE=postgres
POSTGRES_DSN=postgresql+psycopg://username:password@localhost:5432/Stuffing
POSTGRES_QUERY=SELECT * FROM v_hr_employee_attrition
```

Exemple SQL pour construire une vue unique depuis vos tables Stuffing :

```sql
CREATE OR REPLACE VIEW v_hr_employee_attrition AS
SELECT
	ep.id::text AS "EmployeeNumber",
	COALESCE(d.name, 'N/A') AS "Department",
	COALESCE(ep.job_role, 'N/A') AS "JobRole",
	COALESCE(ep.seniority, 'Junior') AS "Seniority",
	COALESCE(ep.contract_type, 'CDI') AS "ContractType",
	COALESCE(ep.monthly_income, 0)::numeric AS "MonthlyIncome",
	COALESCE(ep.performance_rating, 3)::int AS "PerformanceRating",
	COALESCE(ep.job_satisfaction, 3)::int AS "JobSatisfaction",
	COALESCE(ep.environment_satisfaction, 3)::int AS "EnvironmentSatisfaction",
	COALESCE(ep.work_life_balance, 3)::int AS "WorkLifeBalance",
	COALESCE(ep.job_involvement, 3)::int AS "JobInvolvement",
	COALESCE(ep.age, 30)::int AS "Age",
	COALESCE(ep.distance_from_home, 10)::int AS "DistanceFromHome",
	COALESCE(ep.years_at_company, 1)::int AS "YearsAtCompany",
	COALESCE(ep.years_in_current_role, 1)::int AS "YearsInCurrentRole",
	COALESCE(ep.years_since_last_promotion, 0)::int AS "YearsSinceLastPromotion",
	COALESCE(ep.num_companies_worked, 1)::int AS "NumCompaniesWorked",
	COALESCE(ep.total_working_years, 1)::int AS "TotalWorkingYears",
	COALESCE(ep.training_times_last_year, 1)::int AS "TrainingTimesLastYear",
	CASE WHEN COALESCE(ep.is_overtime, false) THEN 'Yes' ELSE 'No' END AS "OverTime",
	EXTRACT(YEAR FROM CURRENT_DATE)::int AS "SnapshotYear",
	COALESCE(abs_stats.absence_days_12m, 0)::int AS "AbsenceDaysLast12Months",
	COALESCE(abs_stats.main_absence_type, 'N/A') AS "MainAbsenceType",
	CASE WHEN u.is_active THEN 'No' ELSE 'Yes' END AS "Attrition"
FROM employee_profiles ep
JOIN users u ON u.id = ep.user_id
LEFT JOIN departements d ON d.id = ep.departement_id
LEFT JOIN (
	SELECT
		lr.employee_profile_id,
		SUM(CASE WHEN lr.status = 'approved' AND lr.start_date >= CURRENT_DATE - INTERVAL '12 months'
				 THEN (lr.end_date - lr.start_date + 1) ELSE 0 END) AS absence_days_12m,
		MODE() WITHIN GROUP (ORDER BY lr.leave_type) AS main_absence_type
	FROM leave_requests lr
	GROUP BY lr.employee_profile_id
) abs_stats ON abs_stats.employee_profile_id = ep.id;
```

Notes:
- Adaptez les noms de colonnes (ex: ep.job_role, ep.contract_type) selon votre schema reel.
- Si une colonne n'existe pas, gardez un COALESCE avec une valeur par defaut compatible.

Le switch est exclusif :
- `DATA_SOURCE=dataset` => PostgreSQL n'est pas utilise.
- `DATA_SOURCE=postgres` => le CSV n'est pas utilise.

3) Lancer le serveur

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Endpoints

- `GET /health`
- `POST /chat`

Payload exemple:

```json
{ "message": "Quels sont les types de congés disponibles ?" }
```
test 
 
