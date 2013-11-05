/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of lightblue.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.redhat.lightblue.metadata.mongo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import com.mongodb.AggregationOutput;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Version;
import com.redhat.lightblue.metadata.MetadataStatus;
import com.redhat.lightblue.metadata.DataStore;
import com.redhat.lightblue.metadata.StatusChange;
import com.redhat.lightblue.metadata.UnknownEntityVersion;

import com.redhat.lightblue.metadata.parser.Extensions;

public class MongoMetadata implements Metadata {

    public static final String DEFAULT_METADATA_COLLECTION="metadata";

    private final DB db;
    private final DBCollection collection;

    private final BSONParser mdParser;

    public MongoMetadata(DB db,
                         String metadataCollection,
                         Extensions parserExtensions) {
        this.db=db;
        this.collection=db.getCollection(metadataCollection);
        this.mdParser=new BSONParser(parserExtensions);
    }

    public MongoMetadata(DB db,
                         Extensions parserExtensions) {
        this(db,DEFAULT_METADATA_COLLECTION,parserExtensions);
    }

    @Override
    public EntityMetadata getEntityMetadata(String entityName, 
                                            String version) {
        if(entityName==null||entityName.length()==0)
            throw new IllegalArgumentException("entityName");
        if(version==null||version.length()==0)
            throw new IllegalArgumentException("version");
        BasicDBObject query=new BasicDBObject("name",entityName).
            append("version.value",version);
        DBObject md=collection.findOne(query);
        if(md!=null)
            return mdParser.parseEntityMetadata(md);
        else
            return null;
    }

    @Override
    public String[] getEntityNames() {
        BasicDBObject groupFields=new BasicDBObject("_id","$entityName");
        BasicDBObject sortFields=new BasicDBObject("_id",1);
        AggregationOutput output=collection.
            aggregate(new BasicDBObject("$group",groupFields),
                      new BasicDBObject("$sort",sortFields));
        Iterable<DBObject> result=output.results();
        ArrayList<String> ret=new ArrayList<String>();
        if(result!=null) {
            for(Iterator<DBObject> itr=result.iterator();itr.hasNext(); ) {
                DBObject obj=itr.next();
                ret.add((String)obj.get("_id"));
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public Version[] getEntityVersions(String entityName) {
        if(entityName==null||entityName.length()==0)
            throw new IllegalArgumentException("entityName");
        BasicDBObject query=new BasicDBObject("entityName",entityName);
        BasicDBObject project=new BasicDBObject("version",1);
        project.append("_id",0);
        DBCursor cursor=collection.find(query,project);
        int n=cursor.count();
        Version[] ret=new Version[n];
        int i=0;
        while(cursor.hasNext()) 
            ret[i++]=mdParser.parseVersion(cursor.next());
        return ret;
    }

    @Override
    public void createNewMetadata(EntityMetadata md) {
        if(md.getName()==null||
           md.getName().length()==0)
            throw new IllegalArgumentException("Empty metadata name");
        if(md.getName().indexOf(' ')!=-1)
            throw new IllegalArgumentException("Invalid metadata name");
        if(md.getFields().getNumChildren()<=0)
            throw new IllegalArgumentException("Metadata without any fields");
        DataStore store=md.getDataStore();
        if(!(store instanceof MongoDataStore) )
            throw new IllegalArgumentException("Invalid datastore");
        Version ver=md.getVersion();
        if(ver==null||ver.getValue()==null||ver.getValue().length()==0)
            throw new IllegalArgumentException("Invalid version");
        if(ver.getValue().indexOf(' ')!=-1)
            throw new IllegalArgumentException("Invalid version number");
        DBObject obj=(DBObject)mdParser.convert(md);
        WriteResult result=collection.insert(obj,WriteConcern.SAFE);
        //// TODO:  deal with error here
    }

    @Override
    public void setMetadataStatus(String entityName,
                                  String version,
                                  MetadataStatus newStatus,
                                  String comment) {
        
        if(entityName==null||entityName.length()==0)
            throw new IllegalArgumentException("entityName");
        if(version==null||version.length()==0)
            throw new IllegalArgumentException("version");
        if(newStatus==null)
            throw new IllegalArgumentException("status");
        BasicDBObject query=new BasicDBObject("name",entityName).
            append("version.value",version);
        DBObject md=collection.findOne(query);
        if(md==null) 
            throw new UnknownEntityVersion(entityName,version);
        EntityMetadata metadata=mdParser.parseEntityMetadata(md);
        
        StatusChange newLog=new StatusChange();
        newLog.setDate(new Date());
        newLog.setStatus(metadata.getStatus());
        newLog.setComment(comment);
        metadata.getStatusChangeLog().add(newLog);
        metadata.setStatus(newStatus);

        query=new BasicDBObject("_id",md.get("_id"));
        WriteResult result=collection.
            update(query,(DBObject)mdParser.convert(metadata),false,false);
        // TODO: deal with errors here
    }
}
