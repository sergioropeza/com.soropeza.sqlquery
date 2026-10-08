-- com.soropeza.sqlquery: index for the statement history of the SQL Query form.
-- Speeds up the history lookup (AD_Issue filtered by form and user, ordered by date).
-- Fails with ORA-00955 if the index already exists (can be ignored).
CREATE INDEX ad_issue_formuser_idx ON AD_Issue (AD_Form_ID, CreatedBy, Created);
