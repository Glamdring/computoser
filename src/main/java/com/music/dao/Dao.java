/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

@Repository("dao")
public class Dao {

    @PersistenceContext
    private EntityManager entityManager;

    public <T> void delete(Class<?> clazz, T id) {
        delete(getById(clazz, id));
    }

    public void delete(Object object) {
        object = entityManager.merge(object);
        entityManager.remove(object);
    }

    public <T> T getById(Class<T> clazz, Object id) {
        return getById(clazz, id, false);
    }

    public <T> T getById(Class<T> clazz, Object id, boolean lock) {
        if (lock) {
            return entityManager.find(clazz, id, LockModeType.PESSIMISTIC_WRITE);
        } else {
            return entityManager.find(clazz, id);
        }
    }

    public <T> T persist(T e) {
        // if e is already in the persistence context (session), no action is
        // taken, except for cascades
        // if e is detached, a copy (e') is returned, which is attached
        // (managed)
        // if e is transient (new instance), it is saved and a persistent (and
        // managed) copy is returned
        e = entityManager.merge(e);

        return e;
    }

    public <T> T getByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue) {
        String dotlessPropertyName = propertyName.replace(".", "");
        List<T> result = findByQuery(new QueryDetails()
                .setQuery("SELECT ob FROM " + clazz.getName() + " ob WHERE " + propertyName
                        + "=:" + dotlessPropertyName)
                .setParamNames(new String[] {dotlessPropertyName})
                .setParamValues(new Object[] {propertyValue}));

        return getResult(result);

    }

    public <T> List<T> getListByPropertyValue(Class<T> clazz, String propertyName,
            Object propertyValue) {
        String dotlessPropertyName = propertyName.replace(".", "");
        List<T> result = findByQuery(new QueryDetails().setQuery(
                "SELECT o FROM " + clazz.getName() + " o WHERE " + propertyName
                        + "=:" + dotlessPropertyName)
                .setParamNames(new String[] { dotlessPropertyName })
                .setParamValues(new Object[] { propertyValue }));

        return result;

    }

    protected int executeQuery(String query, String[] names, Object[] args) {
        if (names == null) {
            names = new String[] {};
        }

        if (args == null) {
            args = new Object[] {};
        }

        Query q = entityManager.createQuery(query);
        for (int i = 0; i < names.length; i++) {
            q.setParameter(names[i], args[i]);
        }
        return q.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> findByQuery(QueryDetails details) {
        Query q = null;
        if (details.getQueryName() != null) {
            q = entityManager.createNamedQuery(details.getQueryName());
        } else if (details.getQuery() != null) {
            q = entityManager.createQuery(details.getQuery());
        } else {
            throw new IllegalArgumentException("Either query or query name must be set");
        }

        for (int i = 0; i < details.getParamNames().length; i++) {
            q.setParameter(details.getParamNames()[i], details.getParamValues()[i]);
        }
        if (details.getStart() > -1) {
            q.setFirstResult(details.getStart());
        }
        if (details.getCount() > -1) {
            q.setMaxResults(details.getCount());
        }
        if (details.isCacheable()) {
            setCacheable(q);
        }
        return q.getResultList();
    }

    protected void setCacheable(Query query) {
        //TODO consider if every query should be cached (hibernate advises against it)
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);
    }

    protected Object getDelegate() {
        return entityManager.getDelegate();
    }

    protected int executeNamedQuery(String name) {
        return entityManager.createNamedQuery(name).executeUpdate();
    }

    protected <T> T getResult(List<T> result) {
        if (!result.isEmpty()) {
             return result.get(0);
         }

         return null;
    }


    public <T> List<T> listOrdered(Class<T> clazz, String orderField) {
        return findByQuery(new QueryDetails().setQuery("from " + clazz.getName() + " ORDER BY "
                + orderField));
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }


    public <T> List<T> getPagedListByPropertyValue(Class<T> clazz,
            String propertyName, Object propertyValue, int page, int pageSize) {
        String queryString = "SELECT o FROM " + clazz.getName() + " o WHERE " + propertyName + "=:" + propertyName;
        TypedQuery<T> query = getEntityManager().createQuery(queryString, clazz);
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);
        query.setParameter(propertyName, propertyValue);
        return query.getResultList();
    }


    public <T> List<T> getOrderedListByPropertyValue(Class<T> clazz,
            String propertyName, Object propertyValue, String orderField) {
        List<T> result = findByQuery(new QueryDetails()
                .setQuery("SELECT o FROM " + clazz.getName()
                + " o WHERE " + propertyName + "=:" + propertyName
                + " ORDER BY " + orderField).setParamNames(new String[] { propertyName })
                .setParamValues(new Object[] { propertyValue }));

        return result;
    }


    public void lock(Object entity) {
        if (entity != null) {
            getEntityManager().lock(entity, LockModeType.PESSIMISTIC_WRITE);
        }
    }


    public <T> List<T> listPaged(Class<T> clazz, int start, int pageSize) {
        return findByQuery(new QueryDetails().setQuery("FROM " + clazz.getName() + " ORDER BY id").setStart(start).setCount(pageSize));
    }

    /**
    * Performs a given operation on all records in batches
    * @param operation
    * @param pageSize
    */

    public <T> void performBatched(Class<T> clazz, int pageSize, PageableOperation<T> operation) {
        int page = 0;
        while (true) {
            List<T> data = listPaged(clazz, page * pageSize, pageSize);
            page++;
            operation.setData(data);
            operation.execute();
            // final batch
            if (data.size() < pageSize) {
                break;
            }
        }
    }

    public <T> List<T> getByIds(Class<T> clazz, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        TypedQuery<T> query = getEntityManager().createQuery("SELECT piece FROM Piece piece WHERE id IN (:ids)", clazz);
        query.setParameter("ids", ids);

        return query.getResultList();
    }
}
