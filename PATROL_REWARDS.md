# Patrol Reward System

This document describes the patrol reward system implemented for managing patrol contests and rewards.

## Features

### Contest Management
- Create patrol contests with configurable time periods and requirements
- Track patrol hours automatically when patrol missions are completed
- Support for afternoon (13:00-18:00) and night (19:00-00:00) time periods
- Daily hour limits (default: 4.5 hours max per day)
- Eligibility requirements (default: 18 hours total)

### Discord Commands

#### `/patrol-contest create`
Creates a new patrol contest.
- **title**: Contest title
- **start-date**: Start date (dd/MM/yyyy format)
- **end-date**: End date (dd/MM/yyyy format)  
- **description**: Optional contest description

#### `/patrol-contest status`
Shows the status of the current active contest, including participant count and configuration.

#### `/patrol-contest check`
Shows your personal patrol hours and eligibility status in the current contest.

#### `/patrol-contest leaderboard`
Shows the top 10 eligible participants ranked by total patrol hours.

#### `/patrol-contest draw`
Performs the lottery drawing to select winners. Can only be run once per contest.

#### `/patrol-contest winners`
Shows the results of the lottery drawing if it has been performed.

## Reward Structure

### Afternoon Rewards (13:00-18:00)
- **Winners**: 3 participants (configurable)
- **Rewards**: 10,000 Diamond packs OR weapon skins

### Night VIP Rewards (19:00-00:00)
- **Winners**: 2 participants (configurable)
- **Rewards**: VIP Diamond (30 days)
- **Eligibility**: 
  - Night patrol participants who meet the hour requirement
  - Afternoon participants who didn't win the main prizes

## How It Works

1. **Contest Creation**: Staff creates a contest with specific dates and requirements
2. **Automatic Tracking**: When patrol missions are completed, hours are automatically tracked and categorized by time period
3. **Daily Limits**: Only up to 4.5 hours per day count toward the contest requirements
4. **Eligibility**: Participants need 18 total hours to be eligible for rewards
5. **Lottery**: Winners are randomly selected from eligible participants
6. **Anti-Farming**: Officers caught idle/farming are automatically disqualified

## Database Schema

### PatrolContest
- Contest configuration and rules
- Time periods and requirements
- Active status and metadata

### PatrolParticipant  
- User progress in contests
- Eligibility status
- Winner flags

### PatrolHours
- Detailed session tracking
- Time period categorization
- Validation flags

## Integration

The system automatically integrates with the existing schedule management:
- When PATROL missions are closed, hours are processed automatically
- Time calculations respect the Fortaleza timezone
- Daily limits are enforced in real-time