-- com.soropeza.sqlquery: index for the statement history of the SQL Query form.
-- Speeds up the history lookup (AD_Issue filtered by form and user, ordered by date).
-- Safe to run more than once.
CREATE INDEX IF NOT EXISTS ad_issue_formuser_idx ON AD_Issue (AD_Form_ID, CreatedBy, Created);
