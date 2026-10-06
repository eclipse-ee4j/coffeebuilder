package ${packageName};

<#if (importsList??) && (importsList?size > 0)>
    <#list importsList as importItem>
import ${importItem};
    </#list>
</#if>
<#assign formId="${instanceModelName}Form" />
<#assign serviceClassName="${modelName}Repository" />
<#assign serviceInstanceName="${instanceModelName}Repository" />
<#assign currentModel="current${modelName}" />
<#assign selectedModels="selected${modelName}s" />
<#assign idNameCap=idName?cap_first />

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.component.UIComponent;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import org.primefaces.PrimeFaces;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

@Named
@ViewScoped
public class ${className} implements Serializable{

    @Inject
    private ${serviceClassName} ${serviceInstanceName};

<#list relations as relation>
    @Inject
    private ${relation.relatedType}Repository ${relation.relatedInstance}Repository;

    private List<${relation.relatedType}> ${relation.relatedOptions};

</#list>

    private ${modelName} ${currentModel};

    private List<${modelName}> ${selectedModels};

    @PostConstruct
    public void init() {
        this.${selectedModels} = new ArrayList<>();
<#list relations as relation>
        this.${relation.relatedOptions} = ${relation.relatedInstance}Repository.findAll();
</#list>
    }

    public ${modelName} getCurrent${modelName}() {
        return ${currentModel};
    }

    public void setCurrent${modelName}(${modelName} ${currentModel}) {
        this.${currentModel} = ${currentModel};
    }

    public List<${modelName}> get${modelName}sList() {
        return ${serviceInstanceName}.findAll();
    }

    public void openNew(){
        this.${currentModel} = new ${modelName}();
    }

    public void save${modelName}(){
        boolean newModel = ${currentModel}.get${idNameCap}() == null;
        ${currentModel} = ${serviceInstanceName}.save(${currentModel});
        if (newModel){
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("${modelName} Added"));
        }else{
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("${modelName} Updated"));
        }
        PrimeFaces.current().executeScript("PF('manage${modelName}Dialog').hide()");
        PrimeFaces.current().ajax().update("${formId}:messages", "${formId}:dt-${instanceModelName}s");
    }

<#list enumFields as field>
    public ${field.enumType}[] get${field.valuesProperty?cap_first}() {
        return ${field.enumType}.values();
    }

</#list>
<#list relations as relation>
    public List<${relation.relatedType}> get${relation.relatedOptions?cap_first}() {
        return ${relation.relatedOptions};
    }

    public Converter<${relation.relatedType}> get${relation.converterProperty?cap_first}() {
        return new Converter<>() {
            @Override
            public ${relation.relatedType} getAsObject(FacesContext context, UIComponent component, String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }
                return ${relation.relatedOptions}.stream()
                    .filter(option -> option.get${relation.relatedId?cap_first}() != null)
                    .filter(option -> option.get${relation.relatedId?cap_first}().toString().equals(value))
                    .findFirst()
                    .orElseThrow(() -> new ConverterException("Unknown ${relation.relatedType} id: " + value));
            }

            @Override
            public String getAsString(FacesContext context, UIComponent component, ${relation.relatedType} value) {
                return value == null || value.get${relation.relatedId?cap_first}() == null
                    ? "" : value.get${relation.relatedId?cap_first}().toString();
            }
        };
    }

</#list>

    public List<${modelName}> getSelected${modelName}s() {
        return ${selectedModels};
    }

    public void setSelected${modelName}s(List<${modelName}> ${selectedModels}) {
        this.${selectedModels} = ${selectedModels};
    }

    public String getDeleteButtonMessage() {
        if (hasSelected${modelName}s()) {
            int size = this.${selectedModels}.size();
            return size > 1 ? size + " ${instanceModelName}s selected" : "1 ${instanceModelName} selected";
        }
        return "Delete";
    }

    public void delete${modelName}() {
        ${serviceInstanceName}.delete(this.${currentModel});
        if (this.${selectedModels} != null) {
            this.${selectedModels}.remove(this.${currentModel});
        }
        this.${currentModel} = null;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("${modelName} Removed"));
        PrimeFaces.current().ajax().update("${formId}:messages", "${formId}:dt-${instanceModelName}s");
    }

    public boolean hasSelected${modelName}s() {
        return this.${selectedModels} != null && !this.${selectedModels}.isEmpty();
    }

    public void deleteSelected${modelName}s() {
        ${serviceInstanceName}.deleteAll(${selectedModels});
        this.${selectedModels} = new ArrayList<>();
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("${modelName}s Removed"));
        PrimeFaces.current().ajax().update("${formId}:messages", "${formId}:dt-${instanceModelName}s");
        PrimeFaces.current().executeScript("PF('dt${modelName}s').clearFilters()");
    }
}
