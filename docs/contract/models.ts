// ─── PERFIL DE USUARIO ────────────────────────────────────────────────────────

export interface UserProfile {
    id: string;
    name: string;
    email: string;
    phone?: string;
    location: string;
    photoUrl?: string;
    availability: 'full-time' | 'part-time' | 'freelance' | 'remote';
    sectors: string[];             // ej: ["Tecnología", "Diseño"]
}

// ─── CV / CURRÍCULUM ──────────────────────────────────────────────────────────

export interface CV {
    id: string;
    userId: string;
    title: string;                 // nombre que le da el usuario al CV
    templateId: string;
    createdAt: string;             // ISO 8601: "2026-09-13T21:00:00Z"
    updatedAt: string;
    personalInfo: UserProfile;
    workExperiences: WorkExperience[];
    education: Education[];
    skills: Skill[];
    languages: Language[];
}

export interface WorkExperience {
    id: string;
    company: string;
    position: string;
    startDate: string;             // "2024-01"
    endDate?: string;              // null si es trabajo actual
    current: boolean;
    description: string;
    // Clave para el módulo de "traducción de experiencia no laboral"
    type: 'formal' | 'freelance' | 'volunteer' | 'school-project' | 'informal';
}

export interface Education {
    id: string;
    institution: string;
    degree: string;
    field: string;
    startDate: string;
    endDate?: string;
    current: boolean;
}

export interface Skill {
    id: string;
    name: string;
    level: 'beginner' | 'intermediate' | 'advanced' | 'expert';
    category: string;              // ej: "Programación", "Diseño", "Idiomas"
}

export interface Language {
    id: string;
    name: string;
    level: 'basic' | 'intermediate' | 'advanced' | 'native';
}

// ─── PLANTILLAS DE CV ─────────────────────────────────────────────────────────

export interface CVTemplate {
    id: string;
    name: string;
    previewUrl: string;
    category: string;
    isPremium: boolean;
    availableOffline: boolean;
}

// ─── OFERTAS LABORALES ────────────────────────────────────────────────────────

export interface JobOffer {
    id: string;
    title: string;
    company: string;
    location: string;
    modality: 'remote' | 'on-site' | 'hybrid';
    type: 'full-time' | 'part-time' | 'freelance' | 'internship';
    sector: string;
    description: string;
    requirements: string[];
    salary?: {
        min: number;
        max: number;
        currency: string;            // "CRC", "USD"
    };
    matchScore?: number;           // 0-100, calculado según el perfil del usuario
    postedAt: string;
}

// ─── TRACKER DE POSTULACIONES (KANBAN) ────────────────────────────────────────

export type ApplicationStatus =
    | 'applied'      // Aplicado
    | 'in-progress'  // En proceso
    | 'interview'    // Entrevista
    | 'rejected'     // Rechazado
    | 'accepted';    // Aceptado

export interface JobApplication {
    id: string;
    userId: string;
    jobOfferId: string;
    jobTitle: string;              // copia del título por si la oferta se elimina
    company: string;
    status: ApplicationStatus;
    appliedAt: string;
    updatedAt: string;
    notes?: string;
}

// ─── ANÁLISIS DE BRECHA DE HABILIDADES ───────────────────────────────────────

export interface SkillsGapAnalysis {
    id: string;
    userId: string;
    targetJobTitle: string;
    missingSkills: string[];
    recommendations: CourseRecommendation[];
    createdAt: string;
}

export interface CourseRecommendation {
    id: string;
    title: string;
    platform: string;              // "YouTube", "Udemy", "Coursera", etc.
    url: string;
    isPaid: boolean;
    duration?: string;             // "4 horas", "6 semanas"
}
