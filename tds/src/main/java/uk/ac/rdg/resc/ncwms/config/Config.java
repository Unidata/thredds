/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.util.Date;
import org.apache.log4j.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Configuration of the server.  We use Simple XML Serialization
 * (http://simple.sourceforge.net/) to convert to and from XML.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="config")
public class Config
{
    private static final Logger logger = Logger.getLogger(Config.class);
    
    // We don't do "private List<Dataset> datasetList..." here because if we do,
    // the config file will contain "<datasets class="java.util.ArrayList>",
    // presumably because the definition doesn't clarify what sort of List should
    // be used.
    
    @Element(name="contact", required=false)
    private Contact contact = new Contact();
    
    @Element(name="server")
    private Server server = new Server();

    /*
    @Element(name="cache", required=false)
    private Cache cache = new Cache();
     \*/
    // Time of the last update to this configuration or any of the contained
    // metadata, in milliseconds since the epoch
    //private long lastUpdateTime = new Date().getTime(); 
    
    //private File configFile; // Location of the file from which this information has been read
    
    //private MetadataStore metadataStore; // Gives access to metadata
    
    /**
     * This contains the map of dataset IDs to Dataset objects.  We use a 
     * ListOrderedMap so that the order of datasets in the Map is preserved.
     */
    //private Map<String, Dataset> datasets = new ListOrderedMap<String, Dataset>();
    
    /**
     * Private constructor.  This prevents other classes from creating
     * new Config objects directly.
     */
    private Config() {}
    
    /**
     * Reads configuration information from the file location given by the
     * provided Context object
     * @param ncwmsContext object containing the context of this ncWMS application
     * (including the location of the config file)
     * @param metadataStore object that is used to read metadata from a store.
     * We need this here to set the state of each Dataset, based upon the last
     * update time.
     */
    /*public static Config readConfig(NcwmsContext ncwmsContext,
        MetadataStore metadataStore) throws Exception
    {
        Config config;
        File configFile = ncwmsContext.getConfigFile();
        if (configFile.exists())
        {
            config = new Persister().read(Config.class, configFile);
            config.configFile = configFile;
            logger.debug("Loaded configuration from {}", configFile.getPath());
        }
        else
        {
            // We must make a new config file and save it
            config = new Config();
            config.configFile = configFile;
            config.save();
            logger.debug("Created new configuration object and saved to {}",
                configFile.getPath());
        }
        
        // Now set the state of each Dataset, based on the last update time in
        // the metadata store
        for (Dataset ds : config.getDatasets().values())
        {
            // Read the time at which this dataset was last updated from the
            // metadata store
            Date lastUpdate = metadataStore.getLastUpdateTime(ds.getId());
            if (lastUpdate == null)
            {
                // The dataset has never been loaded
                ds.setState(State.TO_BE_LOADED);
            }
            else
            {
                // The metadata are already present in the database. We say that this
                // dataset is ready for use, allowing the periodic reloader
                // (see MetadataLoader) to refresh the metadata when necessary.
                ds.setState(State.READY);
            }
        }
        
        // Set the reference to the metadata store, which is needed by the
        // Dataset object to read metadata
        config.metadataStore = metadataStore;
        // The config object is needed by the metadata store when setting
        // properties of the returned Layer objects.
        metadataStore.setConfig(config);
        return config;
    }*/
    
    /**
     * Saves configuration information to the disk.  Other classes can call this
     * method when they have altered the contents of this object.
     * @throws Exception if there was an error saving the configuration
     * @throws IllegalStateException if the config file has not previously been
     * saved.
     */
    /*public void save() throws Exception
    {
        if (this.configFile == null)
        {
            throw new IllegalStateException("No location set for config file");
        }
        new Persister().write(this, this.configFile);
        logger.debug("Config information saved to {}", this.configFile.getPath());
    }*/
    
    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate dataset IDs or duplicate URLs for third-party layer providers.
     */
    /*@Validate
    public void validate() throws PersistenceException
    {
        List<String> dsIds = new ArrayList<String>();
        for (Dataset ds : this.datasetList)
        {
            String dsId = ds.getId();
            if (dsIds.contains(dsId))
            {
                throw new PersistenceException("Duplicate dataset id %s", dsId);
            }
            dsIds.add(dsId);
        }
        for (Dataset ds : this.threddsDatasets)
        {
            String dsId = ds.getId();
            if (dsIds.contains(dsId))
            {
                throw new PersistenceException("Duplicate dataset id %s", dsId);
            }
            dsIds.add(dsId);
        }
    }*/
    
    /**
     * Called when we have checked that the configuration is valid.  Populates
     * the datasets hashmap, loads the datasets from the THREDDS catalog and 
     * populates the hashmap of third-party layer providers.
     */
    /*@Commit
    public void build()
    {
        for (Dataset ds : this.datasetList)
        {
            ds.setConfig(this);
            // The state of the datasets is set based on the contents of the
            // metadata store: see MetadataStore.init()
            this.datasets.put(ds.getId(), ds);
        }
        this.loadThreddsCatalog();
    }
    
    public void setLastUpdateTime(Date date)
    {
        if (date.after(new Date(this.lastUpdateTime)))
        {
            this.lastUpdateTime = date.getTime();
        }
    }*/
    
    /**
     * @return a newly-created Date object representing the time at which this
     * configuration was last updated
     */

    /*
    public Date getLastUpdateTime()
    {
        return new Date(this.lastUpdateTime);
    }
      */
    /**
     * @return the time of the last change to the configuration or metadata,
     * in milliseconds since the epoch
     */

    /*
    public long getLastUpdateTimeMilliseconds()
    {
        return this.lastUpdateTime;
    }
    */

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }


    /*
    public Cache getCache()
    {
        return this.cache;
    }

    public void setCache(Cache cache)
    {
        this.cache = cache;
    } */
    
    public Contact getContact()
    {
        return contact;
    }

    public void setContact(Contact contact)
    {
        this.contact = contact;
    }

    /*
    public Map<String, Dataset> getDatasets()
    {
        return this.datasets;
    } */

    /*
    
    public synchronized void addDataset(Dataset ds)
    {
        ds.setConfig(this);
        this.datasetList.add(ds);
        this.datasets.put(ds.getId(), ds);
    }
    
    public synchronized void removeDataset(Dataset ds)
    {
        this.datasetList.remove(ds);
        this.datasets.remove(ds.getId());
    }
    
    public synchronized void changeDatasetId(String oldId, String newId)
    {
        Dataset ds = this.datasets.remove(oldId);
        ds.setId(newId);
        this.datasets.put(ds.getId(), ds);
    }
    */
    
    /**
     * Used by Dataset to provide a method to get variables
     */
    /*
    MetadataStore getMetadataStore()
    {
        return this.metadataStore;
    } */

    /*
    public void loadThreddsCatalog()
    {
        // First remove the datasets that belonged to the THREDDS catalog.
        for (Dataset ds : this.threddsDatasets)
        {
            this.removeDataset(ds);
        }
        if (this.threddsCatalogLocation != null && !this.threddsCatalogLocation.trim().equals(""))
        {
            logger.debug("Loading datasets from THREDDS catalog at " + this.threddsCatalogLocation);
            try
            {
                this.threddsDatasets = ThreddsConfig.readThreddsDatasets(this.threddsCatalogLocation);

                logger.debug("Number of thredds Datasets: " + this.threddsDatasets.size());

                for(Dataset d : this.threddsDatasets)
                {
                    logger.debug("adding thredds dataset: " + d.getTitle() + " id: " + d.getId());
                    this.addDataset(d);
                }
            }
            catch(Exception e)
            {
                logger.error("Problems loading thredds catalog at " + this.threddsCatalogLocation, e);
            }
        }
    }*/
    
    /**
     * If s is whitespace only or empty, returns a space, otherwise returns s.
     * This is to work around problems with the Simple XML software, which throws
     * an Exception if it tries to read an empty field from an XML file.
     */


    public static String checkEmpty(String s)
    {
        if (s == null) return " ";
        s = s.trim();
        return s.equals("") ? " " : s;
    }

    /*
    public String getThreddsCatalogLocation()
    {
        return this.threddsCatalogLocation;
    }
    
    public void setThreddsCatalogLocation(String threddsCatalogLocation)
    {
        this.threddsCatalogLocation = checkEmpty(threddsCatalogLocation);
    }
         */
    /**
     * Called automatically by Spring.  When we have the application context
     * we can set the admin password in the Users object that is used by Acegi.
     * This is called after the Config object has been created.
     */
    /*
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        // Set the admin password in the Users bean, which we'll need to
        // get from the app context
        Users users = (Users)applicationContext.getBean("users");
        if (users == null)
        {
            logger.error("Could not retrieve Users object from application context");
        }
        else
        {
            logger.debug("Setting admin password in Users object");
            users.setAdminPassword(this.server.getAdminPassword());
        }
    }*/
    
}
