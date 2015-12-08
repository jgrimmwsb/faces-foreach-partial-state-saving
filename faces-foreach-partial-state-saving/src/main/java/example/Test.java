package example;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;

@FacesComponent("example.Test")
public class Test extends UINamingContainer {
	
    @Override
    public void processDecodes(FacesContext context) {
    	process(context, null, () -> super.processDecodes(context));
    }

    @Override
    public void processValidators(final FacesContext context) {
    	process(context, null, () -> super.processValidators(context));
    }

    @Override
    public void processUpdates(final FacesContext context) {
        process(context, null, () -> super.processUpdates(context));
    }

    @Override
    public void processEvent(final ComponentSystemEvent event) throws AbortProcessingException {
    	process(getFacesContext(), null, () -> super.processEvent(event));
    }

    @Override
    public void encodeAll(final FacesContext context) throws IOException {
        process(context, null, () -> super.encodeAll(context));
    }

    @Override
    public void encodeBegin(final FacesContext context) throws IOException {
        process(context, null, () -> super.encodeBegin(context));
    }

    @Override
    public void encodeEnd(final FacesContext context) throws IOException {
        process(context, null, () -> super.encodeEnd(context));
    }

    @Override
    public void encodeChildren(final FacesContext context) throws IOException {
        process(context, null, () -> super.encodeChildren(context));
    }

    @Override
    public void queueEvent(FacesEvent event) {
        super.queueEvent(new TestFacesEvent(event, this));
    }

    @Override
    public boolean visitTree(final VisitContext context, final VisitCallback callback) {
        if (!isVisitable(context)) {
            return false;
        }

        Map<String, Object> originalVars = captureOriginalVars(context.getFacesContext());
        try {
            setVars(context.getFacesContext(), getVariables());
            return super.visitTree(context, callback);
        } finally {
            setVars(context.getFacesContext(), originalVars);
        }
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException {
        if (event instanceof TestFacesEvent) {
            FacesContext context = FacesContext.getCurrentInstance();
            TestFacesEvent contextVariableProvidingFacesEvent = (TestFacesEvent) event;
            final FacesEvent wrapped = contextVariableProvidingFacesEvent.getWrapped();

            process(context, wrapped.getComponent(), () -> wrapped.getComponent().broadcast(wrapped));
        } else {
            super.broadcast(event);
        }
    }

    private void process(FacesContext context, UIComponent targetComponent, Callable callback) {
        Map<String, Object> originalVars = captureOriginalVars(context);

        UIComponent compositeParent = UIComponent.getCompositeComponentParent(targetComponent);
        if (compositeParent != null) {
            pushComponentToEL(context, compositeParent);
        }
        if (targetComponent != null) {
            pushComponentToEL(context, targetComponent);
        }

        try {
            setVars(context, getVariables());
            try {
            	callback.call();
            } catch (Exception e) {
                throw new FacesException(e);
            }
        } finally {
            setVars(context, originalVars);

            if (targetComponent != null) {
                popComponentFromEL(context);
            }
            if (compositeParent != null) {
                popComponentFromEL(context);
            }
        }
    }

    private Map<String, Object> captureOriginalVars(FacesContext context) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        Map<String, Object> originalVars = new LinkedHashMap<>();

        for (Entry<String, Object> variable : getVariables().entrySet()) {
            String varName = variable.getKey();
            if (varName != null) {
                originalVars.put(varName, requestMap.get(varName));
            }
        }

        return originalVars;
    }

    private void setVars(FacesContext context, Map<String, Object> variables) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();

        for (Entry<String, Object> variable : variables.entrySet()) {
            String varName = variable.getKey();
            if (varName != null) {
                Object varValue = variable.getValue();
                if (varValue != null) {
                    requestMap.put(varName, varValue);
                } else {
                    requestMap.remove(varName);
                }
            }
        }
    }
	
    private Map<String, Object> getVariables() {
        String varName = (String) getAttributes().get("var");
        Object currentValue = getAttributes().get("value");

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(varName, currentValue);

        return Collections.unmodifiableMap(variables);
    }
    
    interface Callable {
    	
    	void call() throws Exception;
    	
    }

    static final class TestFacesEvent extends FacesEvent implements FacesWrapper<FacesEvent> {

        private static final long serialVersionUID = 1L;

        private final FacesEvent wrapped;

        public TestFacesEvent(FacesEvent wrapped, UIComponent component) {
            super(component);
            this.wrapped = wrapped;
        }

        @Override
        public void queue() {
            wrapped.queue();
        }

        @Override
        public boolean isAppropriateListener(FacesListener listener) {
            return wrapped.isAppropriateListener(listener);
        }

        @Override
        public void processListener(final FacesListener listener) {
            Test component = (Test) wrapped.getComponent();
            component.process(FacesContext.getCurrentInstance(), null, () -> wrapped.processListener(listener));
        }

        @Override
        public PhaseId getPhaseId() {
            return wrapped.getPhaseId();
        }

        @Override
        public void setPhaseId(PhaseId phaseId) {
            wrapped.setPhaseId(phaseId);
        }

        @Override
        public FacesEvent getWrapped() {
            return wrapped;
        }

    }
	
}
