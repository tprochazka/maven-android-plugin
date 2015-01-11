package com.jayway.maven.plugins.android.configuration;

import com.jayway.maven.plugins.android.standalonemojos.CompatibleScreen;
import com.jayway.maven.plugins.android.standalonemojos.SupportsScreens;

import java.util.List;
import java.util.Properties;

/**
 * Configuration for the manifest update. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @deprecated Use ManifestMerger {@link com.jayway.maven.plugins.android.configuration.ManifestMerger} in combination
 * with {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo}
 */
@Deprecated
public class Manifest
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestVersionName}.
     */
    protected String versionName;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestVersionCode}.
     */
    protected Integer versionCode;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo
     * #manifestVersionCodeAutoIncrement}.
     */
    private Boolean versionCodeAutoIncrement;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo
     * #manifestVersionCodeUpdateFromVersion}.
     */
    protected Boolean versionCodeUpdateFromVersion;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestApplicationIcon}.
     */
    protected String applicationIcon;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestApplicationLabel}.
     */
    protected String applicationLabel;    
    
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestApplicationTheme}.
     */
    protected String applicationTheme;    
    
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestSharedUserId}.
     */
    protected String sharedUserId;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestDebuggable}.
     */
    protected Boolean debuggable;

    /**
     * Mirror of
     * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestSupportsScreens}
     * .
     */
    protected SupportsScreens supportsScreens;

    /**
     * Mirror of
     * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestCompatibleScreens}
     * .
     */
    protected List<CompatibleScreen> compatibleScreens;

    /**
     * Mirror of
     * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestProviderAuthorities}
     * .
     */
    protected Properties providerAuthorities;

    /**
     * Mirror of
     * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestUsesSdk}
     */
    protected UsesSdk usesSdk;

    public String getVersionName()
    {
        return versionName;
    }

    public Integer getVersionCode()
    {
        return versionCode;
    }

    public Boolean getVersionCodeAutoIncrement()
    {
        return versionCodeAutoIncrement;
    }

    public Boolean getVersionCodeUpdateFromVersion()
    {
        return versionCodeUpdateFromVersion;
    }

    public String getApplicationIcon() 
    {
    return applicationIcon; 
    }
    
    public String getApplicationLabel() 
    {
        return applicationLabel;
    }

    public String getApplicationTheme() 
    {
        return applicationTheme;
    }

    public String getSharedUserId()
    {
        return sharedUserId;
    }

    public Boolean getDebuggable()
    {
        return debuggable;
    }

    public SupportsScreens getSupportsScreens()
    {
        return supportsScreens;
    }

    public List<CompatibleScreen> getCompatibleScreens()
    {
        return compatibleScreens;
    }

    public Properties getProviderAuthorities()
    {
        return providerAuthorities;
    }

    public UsesSdk getUsesSdk()
    {
        return usesSdk;
    }
}
