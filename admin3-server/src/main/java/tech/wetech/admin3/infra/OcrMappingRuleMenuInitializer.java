package tech.wetech.admin3.infra;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static tech.wetech.admin3.common.Constants.RESOURCE_ROOT_ID;

@Component
public class OcrMappingRuleMenuInitializer implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  public OcrMappingRuleMenuInitializer(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!existsResource(RESOURCE_ROOT_ID)) return;

    Long menuId = ensureResource("\u6620\u5c04\u89c4\u5219\u7ba1\u7406", 0, "/ocr-mapping-rules", "Connection", "ocr-mapping-rule:view", RESOURCE_ROOT_ID);
    Long updateId = ensureResource("\u7f16\u8f91\u6620\u5c04\u89c4\u5219", 1, null, null, "ocr-mapping-rule:update", menuId);
    Long deleteId = ensureResource("\u5220\u9664\u6620\u5c04\u89c4\u5219", 1, null, null, "ocr-mapping-rule:delete", menuId);

    grantAdminRoles(menuId);
    grantAdminRoles(updateId);
    grantAdminRoles(deleteId);
  }

  private boolean existsResource(Long id) {
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource WHERE id = ?", Integer.class, id);
    return count != null && count > 0;
  }

  private Long ensureResource(String name, int type, String url, String icon, String permission, Long parentId) {
    Long id = findResourceId(permission);
    if (id == null) {
      jdbcTemplate.update(
        "INSERT INTO resource (icon, name, parent_ids, permission, type, url, parent_id) VALUES (?, ?, NULL, ?, ?, ?, ?)",
        icon, name, permission, type, url, parentId);
      return findResourceId(permission);
    }
    jdbcTemplate.update(
      "UPDATE resource SET icon = ?, name = ?, type = ?, url = ?, parent_id = ? WHERE id = ?",
      icon, name, type, url, parentId, id);
    return id;
  }

  private Long findResourceId(String permission) {
    return jdbcTemplate.query("SELECT id FROM resource WHERE permission = ? ORDER BY id LIMIT 1",
      rs -> rs.next() ? rs.getLong("id") : null,
      permission);
  }

  private void grantAdminRoles(Long resourceId) {
    if (resourceId == null) return;
    jdbcTemplate.update("""
      INSERT INTO role_resource (role_id, resource_id)
      SELECT role.id, ?
      FROM role
      WHERE (role.name = 'admin' OR role.name LIKE ?)
        AND NOT EXISTS (
          SELECT 1 FROM role_resource rr
          WHERE rr.role_id = role.id AND rr.resource_id = ?
        )
      """, resourceId, "%\u7ba1\u7406\u5458%", resourceId);
  }
}
