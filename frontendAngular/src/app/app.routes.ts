import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [
	{
		path: '',
		loadComponent: () =>
			import('./features/home/home.component').then((m) => m.HomeComponent),
	},
	{
		path: 'admin',
		canActivate: [authGuard],
		data: { roles: ['ADMIN'] },
		loadComponent: () =>
			import('./features/rh/admin-layout/admin-layout.component').then(
				(m) => m.AdminLayoutComponent
			),
		children: [
			{ path: '', redirectTo: 'requests', pathMatch: 'full' },
			{
				path: 'requests',
				loadComponent: () =>
					import(
						'./features/rh/account-requests-dashboard/account-requests-dashboard.component'
					).then((m) => m.AccountRequestsDashboardComponent),
			},
			{
				path: 'users',
				loadComponent: () =>
					import('./features/rh/users/users.component').then(
						(m) => m.UsersComponent
					),
			},
			{
				path: 'departements',
				loadComponent: () =>
					import('./features/rh/departments/departements.component').then(
						(m) => m.DepartementsComponent
					),
			},
			{
				path: 'reports',
				loadComponent: () =>
					import('./features/rh/reports/reports.component').then(
						(m) => m.AdminReportsComponent
					),
			},
			{
				path: 'settings',
				loadComponent: () =>
					import('./features/rh/settings/settings.component').then(
						(m) => m.SettingsComponent
					),
			},
		],
	},
	{
		path: 'manager',
		canActivate: [authGuard],
		data: { roles: ['DELIVERY_MANAGER'] },
		loadComponent: () =>
			import(
				'./features/manager/manager-layout/manager-layout.component'
			).then((m) => m.ManagerLayoutComponent),
		children: [
			{ path: '', redirectTo: 'dashboard', pathMatch: 'full' },
			{
				path: 'dashboard',
				loadComponent: () =>
					import(
						'./features/manager/dashboard/dashboard.component'
					).then((m) => m.ManagerDashboardComponent),
			},
			{
				path: 'team',
				loadComponent: () =>
					import('./features/manager/team/team.component').then(
						(m) => m.TeamComponent
					),
			},
			{
				path: 'projects',
				loadComponent: () =>
					import(
						'./features/manager/projects/projects.component'
					).then((m) => m.ProjectsComponent),
			},
			{
				path: 'planning',
				loadComponent: () =>
					import(
						'./features/manager/planning/planning.component'
					).then((m) => m.PlanningComponent),
			},
			{
				path: 'leaves',
				loadComponent: () =>
					import(
						'./features/manager/leaves/leaves.component'
					).then((m) => m.ManagerLeavesComponent),
			},
			{
				path: 'analytics',
				loadComponent: () =>
					import(
						'./features/manager/analytics/analytics.component'
					).then((m) => m.AnalyticsComponent),
			},
		],
	},
	{
		path: 'collaborator',
		canActivate: [authGuard],
		data: { roles: ['COLLABORATEUR'] },
		loadComponent: () =>
			import(
				'./features/collaborator/collaborator-layout/collaborator-layout.component'
			).then((m) => m.CollaboratorLayoutComponent),
		children: [
			{ path: '', redirectTo: 'dashboard', pathMatch: 'full' },
			{
				path: 'dashboard',
				loadComponent: () =>
					import(
						'./features/collaborator/dashboard/dashboard.component'
					).then((m) => m.CollabDashboardComponent),
			},
			{
				path: 'assignments',
				loadComponent: () =>
					import(
						'./features/collaborator/assignments/assignments.component'
					).then((m) => m.CollabAssignmentsComponent),
			},
			{
				path: 'calendar',
				loadComponent: () =>
					import(
						'./features/collaborator/calendar/calendar.component'
					).then((m) => m.CollabCalendarComponent),
			},
			{
				path: 'leaves',
				loadComponent: () =>
					import(
						'./features/collaborator/leaves/leaves.component'
					).then((m) => m.CollabLeavesComponent),
			},
			{
				path: 'profile',
				loadComponent: () =>
					import(
						'./features/collaborator/profile/profile.component'
					).then((m) => m.CollabProfileComponent),
			},
		],
	},
	{
		path: 'auth/register',
		loadComponent: () =>
			import('./features/auth/register/register.component').then(
				(m) => m.RegisterComponent
			),
	},
	{
		path: 'chatbot',
		canActivate: [authGuard],
		loadComponent: () =>
			import('./features/chatbot/chatbot.component').then(
				(m) => m.ChatbotComponent
			),
	},
	{ path: '**', redirectTo: '' },
];
