/**
 * Mission types available for operations
 * @readonly
 * @enum {string}
 */
const MissionType = Object.freeze({
  CAS: 'CAS',           // Close Air Support
  CAP: 'CAP',           // Combat Air Patrol
  SEAD: 'SEAD',         // Suppression of Enemy Air Defenses
  STRIKE: 'STRIKE',     // Strike Mission
  TRANSPORT: 'TRANSPORT', // Transport Mission
  TRAINING: 'TRAINING', // Training Mission
  ACTION: 'ACTION',     // Action Mission
  OUTROS: 'OUTROS'      // Other/Custom Mission
});

export default MissionType;