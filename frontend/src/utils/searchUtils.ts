import Fuse from 'fuse.js';
import crimesData from '../data/crimes.json';
import locationsData from '../data/locations.json';
import type { Crime, AutocompleteOption } from '../types';

/**
 * Fuzzy search configuration
 */
const fuseOptions = {
  keys: ['crime', 'description'],
  threshold: 0.4, // 0 = exact match, 1 = match anything
  includeScore: true,
  minMatchCharLength: 2,
  ignoreLocation: true,
};

const locationFuseOptions = {
  threshold: 0.3,
  includeScore: true,
  minMatchCharLength: 2,
  ignoreLocation: true,
};

/**
 * Initialize Fuse instances
 */
const crimesFuse = new Fuse(crimesData as Crime[], fuseOptions);
const locationsFuse = new Fuse(locationsData as string[], locationFuseOptions);

/**
 * Search crimes with fuzzy matching
 */
export const searchCrimes = (query: string): Crime[] => {
  if (!query || query.length < 2) return [];
  
  const results = crimesFuse.search(query);
  return results.map(result => result.item);
};

/**
 * Search locations with fuzzy matching
 */
export const searchLocations = (query: string): string[] => {
  if (!query || query.length < 2) return [];
  
  const results = locationsFuse.search(query);
  return results.map(result => result.item);
};

/**
 * Get all crimes for initial load
 */
export const getAllCrimes = (): Crime[] => {
  return crimesData as Crime[];
};

/**
 * Get all locations for initial load
 */
export const getAllLocations = (): string[] => {
  return locationsData as string[];
};

/**
 * Convert locations to autocomplete options
 */
export const locationsToOptions = (locations: string[]): AutocompleteOption[] => {
  return locations.map(location => ({
    label: location,
  }));
};

/**
 * Convert crimes to autocomplete options
 */
export const crimesToOptions = (crimes: Crime[]): AutocompleteOption[] => {
  return crimes.map(crime => ({
    label: crime.crime,
    description: crime.description,
  }));
};