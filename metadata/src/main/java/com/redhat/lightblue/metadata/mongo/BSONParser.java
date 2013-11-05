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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import com.mongodb.BasicDBObject;

import org.bson.BSONObject;

import com.redhat.lightblue.util.Error;

import com.redhat.lightblue.metadata.MetadataParser;

import com.redhat.lightblue.metadata.parser.Extensions;

public class BSONParser extends MetadataParser<BSONObject> {

    public BSONParser(Extensions<BSONObject> ex) {
        super(ex);
    }

    @Override
    protected String getStringProperty(BSONObject object,String name) {
        Object x=object.get(name);
        if(x!=null)
            if(x instanceof String)
                return (String)x;
            else
                throw Error.get(MetadataParser.ILL_FORMED_MD,name);
        else
            return null;
    }

    @Override
    protected BSONObject getObjectProperty(BSONObject object,String name) {
        Object x=object.get(name);
        if(x!=null)
            if(x instanceof BSONObject)
                return (BSONObject)x;
            else
                throw Error.get(MetadataParser.ILL_FORMED_MD,name);
        else
            return null;
    }

    @Override
    protected List<String> getStringList(BSONObject object,String name) {
        Object x=object.get(name);
        if(x!=null)
            if(x instanceof List) {
                ArrayList<String> ret=new ArrayList<String>();
                for(Object o:(List)x)
                    ret.add(o.toString());
                return ret;
            } else
                throw Error.get(MetadataParser.ILL_FORMED_MD,name);
        else
            return null;
    }

    @Override
    protected List<BSONObject> getObjectList(BSONObject object,String name) {
        Object x=object.get(name);
        if(x!=null) {
            if(x instanceof List)
                return (List<BSONObject>)x;
            else
                throw Error.get(MetadataParser.ILL_FORMED_MD,name);
        } else
            return null;
    }

    @Override
    protected BSONObject newNode() {
        return new BasicDBObject();
    }

    @Override
    protected Set<String> getChildNames(BSONObject object) {
        return object.keySet();
    }

    @Override
    protected void putString(BSONObject object,String name,String value) {
        object.put(name,value);
    }

    @Override
    protected void putObject(BSONObject object,String name,Object value) {
        object.put(name,value);
    }

    @Override
    protected  Object newArrayField(BSONObject object,String name) {
        Object ret=new ArrayList();
        object.put(name,ret);
        return ret;
    }

    @Override
    protected void addStringToArray(Object array,String value) {
        ((List)array).add(value);
    }

    @Override
    protected void addObjectToArray(Object array,Object value) {
        ((List)array).add(value);
    }
}
