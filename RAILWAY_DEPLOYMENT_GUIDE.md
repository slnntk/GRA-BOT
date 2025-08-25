# Railway Deployment Guide - Optimized GRA Bot

## Quick Deployment Steps

### 1. Environment Variables (Required)
Set in your Railway service:

```bash
# Essential
DISCORD_BOT_TOKEN=your_actual_discord_bot_token

# Optional performance tuning (recommended)
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:+UseStringDeduplication
SPRING_PROFILES_ACTIVE=production

# Optional image URLs (fallbacks provided)
GRA_IMAGE_URL=https://your-custom-gra-image.png
FOOTER_GRA_BLUE_URL=https://your-custom-footer.png
FOOTER_CHOOSE_HELI_FIRST_SCREEN_URL=https://your-custom-heli-screen.png
AIRCRAFT_VALKYRE_URL=https://your-valkyre-image.png
AIRCRAFT_EC135_URL=https://your-ec135-image.png
AIRCRAFT_MAVERICK_URL=https://your-maverick-image.png
AIRCRAFT_VECTREII_URL=https://your-vectreii-image.png
```

### 2. Railway Service Configuration
- **Port**: Automatic (Railway will set)
- **Health Check Path**: `/actuator/health` (if enabled)
- **Build Command**: `mvn clean package -DskipTests`
- **Start Command**: `java -jar target/aviation-discord-bot-*.jar`

### 3. Dockerfile Options

#### Option A: Use Railway's Auto-Build (Recommended)
Railway will automatically detect the Maven project and build it.

#### Option B: Use Custom Dockerfile
Copy `Dockerfile.railway` to root as `Dockerfile` for custom container builds.

## Expected Resource Usage

### Memory
- **Before Optimization**: ~527MB
- **After Optimization**: ~393MB
- **Railway Cost Impact**: ~25% reduction in memory-based charges

### CPU
- **Estimated Reduction**: 30-40% lower CPU usage
- **Benefits**: Better performance during traffic spikes

### Build Time
- **Before**: 106+ seconds
- **After**: ~12-15 seconds
- **CI/CD Impact**: Faster deployments, lower build costs

## Monitoring & Verification

### After Deployment, Check:

1. **Memory Usage** (Railway Dashboard)
   - Should stabilize around 300-400MB
   - No memory leaks or spikes above 384MB

2. **CPU Usage**
   - Should average <30% during normal operation
   - Lower baseline usage compared to before

3. **Application Logs**
   - Look for: "Using G1 garbage collector"
   - Look for: "Cache cleanup executor initialized"
   - Ensure no ERROR messages on startup

4. **Discord Functionality**
   - Test `/setup-escala` command
   - Test creating and managing schedules
   - Verify all interactions work smoothly

### Key Success Indicators

```log
# Good signs in logs:
INFO --- Using H2 database with optimized connection pool
INFO --- GRA-Bot- thread pool initialized with 2 core threads
INFO --- Bot Discord iniciado com sucesso!

# Memory efficiency:
DEBUG --- Cache cleanup executado. Entradas removidas: X, restantes: Y
```

## Troubleshooting

### High Memory Usage
If memory usage is higher than expected:
1. Check for memory leaks in Discord connections
2. Verify cache cleanup is running
3. Consider reducing max heap: `-Xmx320m`

### Performance Issues
If bot seems slower:
1. Increase thread pool: `executor.setCorePoolSize(3)`  
2. Reduce cache cleanup interval back to 10 minutes
3. Enable more detailed logging temporarily

### Database Issues  
If H2 database problems:
1. Check `/app/data/` directory permissions
2. Verify no disk space issues
3. Consider PostgreSQL fallback if needed

## Rollback Plan

If issues occur, quickly revert by setting:
```bash
# Revert JVM optimizations
JAVA_TOOL_OPTIONS=-Xms256m -Xmx512m

# Enable debug logging  
SPRING_PROFILES_ACTIVE=debug
```

Then redeploy with previous settings.

## Cost Impact Analysis

### Monthly Savings Estimate
- **Memory**: 25% reduction = $2-8/month savings
- **CPU**: 30% reduction = $1-5/month savings  
- **Build Time**: 88% faster = CI/CD cost reduction
- **Total Estimated**: $5-15/month savings

### ROI Timeline
- **Immediate**: Faster deployments and lower resource usage
- **1 Week**: Confirmed memory/CPU baseline improvements
- **1 Month**: Full cost impact visible in Railway billing

## Next Optimization Opportunities

1. **Image Caching**: Cache Discord images locally (~$1-2/month bandwidth savings)
2. **Database Optimization**: More aggressive cleanup policies
3. **Native Image**: Explore GraalVM for further memory reduction
4. **Connection Pooling**: Consider R2DBC for reactive database access

---

## Support & Monitoring

Monitor these metrics in Railway dashboard:
- **Memory**: Target <400MB average
- **CPU**: Target <30% average  
- **Build Time**: Target <20 seconds
- **Response Time**: Discord interactions <2 seconds

For issues or questions about the optimizations, refer to `RESOURCE_OPTIMIZATION.md`.