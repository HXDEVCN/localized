package de.malkusch.localized.spi;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.AbstractEvent;

import de.malkusch.localized.LocalizedDAO;
import de.malkusch.localized.LocalizedProperty;
import de.malkusch.localized.LocalizedUtil;
import de.malkusch.localized.configuration.DefaultLocalizedConfiguration;
import de.malkusch.localized.configuration.LocalizedConfiguration;
import de.malkusch.localized.exception.LocalizedException;

/**
 * Base class for event handling.
 * 
 * This class dispatches the event to the {@literal @Localized} fields of the
 * event's entity.
 * 
 * @author Markus Malkusch <markus@malkusch.de>
 */
abstract public class AbstractEventListener {
	
	protected static LocalizedConfiguration configuration = new DefaultLocalizedConfiguration();
	
	protected SessionFactoryImplementor sessionFactory;

	public AbstractEventListener(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * Handles the event for each {@literal @Localized} field.
	 * 
	 * This method has a separate {@link Session} as Hibernate's event system
	 * seems to break when using the same Session. The session runs on the same
	 * {@link Connection}, i.e. in the same transaction.
	 */
	abstract protected void handleField(StatelessSession session, Field field, Object entity, LocalizedProperty property) throws LocalizedException;
	
	/**
	 * Dispatches an event for each {@literal @Localized} field.
	 * 
	 * The Hibernate event callback should be forwarded to this method.
	 * 
	 * @see #handleField(StatelessSession, Field, Object, LocalizedProperty)
	 */
	protected <T extends AbstractEvent> void handleFields(T event, Object entity, Serializable id) {
		if (entity instanceof LocalizedProperty) {
			return;

		}
		StatelessSession session = sessionFactory.openStatelessSession(event.getSession().connection());
		try {
			Locale locale = configuration.resolveLocale(event.getSession());
			LocalizedDAO dao = new LocalizedDAO(session);
			
			for (Field field : LocalizedUtil.getLocalizedFields(entity.getClass())) {
				LocalizedProperty property = dao.find(entity.getClass(), field.getName(), locale, id);
				if (property == null) {
					property = new LocalizedProperty();
					property.setField(field.getName());
					property.setLocale(locale);
					property.setType(entity.getClass());
					property.setInstance(id.toString());
					
				}
				handleField(session, field, entity, property);
				
			}
		} catch (LocalizedException e) {
			throw new IllegalStateException(e);
			
		} finally {
			session.close();
			
		}
	}
	
	/**
	 * Sets the shared configuration.
	 * 
	 * You should not call this method. The {@link LocalizedConfiguration} instance
	 * will do this.
	 * 
	 * @see LocalizedConfiguration#LocalizedConfiguration()
	 */
	public static void setConfiguration(LocalizedConfiguration configuration) {
		AbstractEventListener.configuration = configuration;
	}
	
}
