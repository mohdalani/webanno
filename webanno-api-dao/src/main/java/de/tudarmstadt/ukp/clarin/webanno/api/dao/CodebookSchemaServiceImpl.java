/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt  
 *  and Language Technology Group  Universität Hamburg 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static java.util.Objects.isNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

/**
 * Implementation of methods defined in the {@link AnnotationSchemaService} interface
 */
@Component(CodebookSchemaService.SERVICE_NAME)
public class CodebookSchemaServiceImpl
    implements CodebookSchemaService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${repository.path}")
    private File dir;

    private @PersistenceContext EntityManager entityManager;
    private @Lazy @Autowired(required = false) List<ProjectInitializer> initializerProxy;
    private @Autowired FeatureSupportRegistry featureSupportRegistry;
    public CodebookSchemaServiceImpl()
    {
        // Nothing to do
    }


    @Override
    @Transactional
    public void createCodebook(Codebook codebook)
    {
        if (isNull(codebook.getId())) {
            entityManager.persist(codebook);
        }
        else {
            entityManager.merge(codebook);
        }
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(codebook.getProject().getId()))) {
            Project project = codebook.getProject();
            log.info("Created codebook [{}]({}) in project [{}]({})", codebook.getName(),
                    codebook.getId(), project.getName(), project.getId());
        }
    }
    
    
    @Override
    @Transactional
    public void createCodebookFeature(CodebookFeature aFeature)
    {
        if (isNull(aFeature.getId())) {
            entityManager.persist(aFeature);
        }
        else {
            entityManager.merge(aFeature);
        }
    }
    
    
    @Override
    @Transactional
    public boolean existsFeature(String aName, Codebook aCodebook)
    {

        try {
            entityManager
                    .createQuery("FROM CodebookFeature WHERE name = :name AND codebook = :codebook",
                            CodebookFeature.class).setParameter("name", aName)
                    .setParameter("codebook", aCodebook).getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;

        }
    }
    
    

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsCodebook(String aName, Project aProject)
    {
        try {
            entityManager
                    .createQuery(
                            "FROM Codebook WHERE name = :name AND project = :project",
                            Codebook.class)
                    .setParameter("name", aName)
                    .setParameter("project", aProject)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException e) {
            return false;
        }
    }
    @Override
    @Transactional
    public Codebook getCodebook(long aId)
    {
        return entityManager
                .createQuery("FROM Codebook WHERE id = :id", Codebook.class)
                .setParameter("id", aId).getSingleResult();
    }
    
    
    
    @Override
    @Transactional
    public Codebook getCodebook(int aCodebookorder, Project aProject) {
        return entityManager
                .createQuery(
                        "FROM Codebook WHERE codebookorder = :codebookorder AND project =:project ",
                        Codebook.class)
                .setParameter("codebookorder", aCodebookorder).setParameter("project", aProject)
                .getSingleResult();
    }
    
    
    @Override
    @Transactional
    public Codebook getCodeBook(String aName, Project aProject)
    {
        return entityManager
                .createQuery("From Codebook where name = :name AND project =:project",
                        Codebook.class).setParameter("name", aName)
                .setParameter("project", aProject).getSingleResult();
    }
   

    @Override
    @Transactional
    public  CodebookFeature getCodebookFeature(String aName, Codebook aCodebook)
    {
        return entityManager
                .createQuery("From CodebookFeature where name = :name AND codebook = :codebook",
                        CodebookFeature.class).setParameter("name", aName)
                .setParameter("codebook", aCodebook).getSingleResult();
    }
    

    @Override
    @Transactional
    public List<Codebook> listCodebook(Project aProject)
    {
        return entityManager
                .createQuery("FROM Codebook WHERE project =:project ORDER BY codebookorder asc",
                        Codebook.class).setParameter("project", aProject).getResultList();       
    }


    @Override
    @Transactional
    public List<CodebookFeature> listCodebookFeature(Codebook code)
    {
        if (isNull(code) || isNull(code.getId())) {
            return new ArrayList<>();
        }

        return entityManager
                .createQuery("FROM CodebookFeature  WHERE codebook =:codebook ORDER BY uiName",
                        CodebookFeature.class).setParameter("codebook", code).getResultList();
    }

    
    @Override
    @Transactional
    public List<CodebookFeature> listCodebookFeature(Project aProject)
    {
        return entityManager
                .createQuery(
                        "FROM CodebookFeature f WHERE project =:project ORDER BY f.codebook.uiName, f.uiName",
                        CodebookFeature.class).setParameter("project", aProject).getResultList();
    }
   

    @Override
    @Transactional
    public void removeCodebook(Codebook aCodebook)
    {
        for (CodebookFeature feature : listCodebookFeature(aCodebook)) {
            
            removeCodebookFeature(feature);
        }
        
        entityManager.remove(
                entityManager.contains(aCodebook) ? aCodebook : entityManager.merge(aCodebook));
    }
    
    
    public void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD,
            Codebook aCodebook)
    {
        List<CodebookFeature> features = listCodebookFeature(aCodebook);
        for (CodebookFeature feature : features) {
            FeatureSupport fs = featureSupportRegistry.getFeatureSupport(feature);
            fs.generateFeature(aTSD, aTD, feature);
        }
    }


    @Override
    public void removeCodebookFeature(CodebookFeature aFeature) {
        entityManager.remove(
                entityManager.contains(aFeature) ? aFeature : entityManager.merge(aFeature));
        
    }
    
    
}

