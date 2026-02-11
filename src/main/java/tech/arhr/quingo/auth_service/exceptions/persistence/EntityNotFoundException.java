package tech.arhr.quingo.auth_service.exceptions.persistence;

public class EntityNotFoundException extends PersistenceException {
  public EntityNotFoundException(String message) {
    super(message);
  }
}
