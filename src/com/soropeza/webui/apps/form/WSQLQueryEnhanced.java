/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Carlos Ruiz - globalqss - bxservice                               *
 * - Sergio Oropeza - com.soropeza - UX improvements                   *
 **********************************************************************/

package com.soropeza.webui.apps.form;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.DBException;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.ToolBar;
import org.adempiere.webui.component.ToolBarButton;
import org.adempiere.webui.component.WListItemRenderer;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MIssue;
import org.compiere.model.MSysConfig;
import org.compiere.model.SystemIDs;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.Notification;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Frozen;
import org.zkoss.zul.North;
import org.zkoss.zul.Separator;
import org.zkoss.zul.South;

/**
 * A Custom Form to process SQL queries.
 *
 * The statement to be executed can be restricted using SysConfig.
 */
@org.idempiere.ui.zk.annotation.Form
public class WSQLQueryEnhanced extends ADForm implements EventListener<Event>
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6641250848300700313L;

	/** Log. */
	private static final CLogger  log = CLogger.getCLogger(WSQLQueryEnhanced.class);

	/** Echo event to execute the statement showing a busy indicator. */
	private static final String EVENT_ON_EXECUTE_SQL = "onExecuteSQL";
	/** Client side event fired by Ctrl+Enter on the SQL editor. */
	private static final String EVENT_ON_CTRL_ENTER = "onCtrlEnter";
	/** Maximum number of statements kept in the history. */
	private static final int MAX_HISTORY = 50;
	/** Base style of the status/result field. */
	private static final String STATUS_STYLE = "font-family: monospace; font-size: 12px; border: none; background-color: transparent;";

	/** Grid used to layout components. */
	private Borderlayout layout = new Borderlayout();
	/** SQL statement field. */
	private Textbox m_txbSqlField = new Textbox();
	/** Execute button. */
	private ToolBarButton m_btnExecute = new ToolBarButton();
	/** Clear editor button. */
	private ToolBarButton m_btnClear = new ToolBarButton();
	/** Copy results to clipboard button. */
	private ToolBarButton m_btnCopy = new ToolBarButton();
	/** Export results to CSV button. */
	private ToolBarButton m_btnExport = new ToolBarButton();
	/** Previously executed statements. */
	private Combobox m_cbHistory = new Combobox();
	/** Field to hold result of SQL statement execution. */
	private Textbox m_txbResultField = new Textbox();
	/** Grid to show the result data. */
	private ListModelTable model = null;
	/** Column headers of the last successful execution (first entry is the row counter). */
	private List<String> m_header = null;
	private WListbox listbox = new WListbox();
	/** Statement history, most recent first. */
	private List<String> m_history = new ArrayList<String>();

	/**
	 * Remove comment
	 */
	private static final String REGEX_REMOVE_COMMENTS = "/\\*(?:.|[\\n\\r])*?\\*/";

	/**
	 * Remove quoted string
	 */
	private static final String REGEX_REMOVE_QUOTED_STRINGS = "'(?:.|[\\n\\r])*?'";

	/**
	 * Remove leading space
	 */
	private static final String REGEX_REMOVE_LEADING_SPACES = "^\\s+";

	/**
	 * Default constructor.
	 */
	public WSQLQueryEnhanced() {
		super();
	}

	/**
	 * Layout form
	 */
	@Override
	protected void initForm() {
		North north = new North();
		Center center = new Center();
		South south = new South();
		final int maxStatementLength = 10000;

		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		layout.setStyle("background-color: transparent; position: relative;");

		// editor region: toolbar + resizable SQL editor
		Div editorPanel = new Div();
		ZKUpdateUtil.setHeight(editorPanel, "100%");
		editorPanel.appendChild(createToolbar());

		m_txbSqlField.setMultiline(true);
		m_txbSqlField.setMaxlength(maxStatementLength);
		ZKUpdateUtil.setHflex(m_txbSqlField, "1");
		ZKUpdateUtil.setVflex(m_txbSqlField, "1");
		m_txbSqlField.setReadonly(false);
		m_txbSqlField.setStyle("font-family: monospace; font-size: 13px; resize: none;");
		m_txbSqlField.setPlaceholder("SELECT ...   (Ctrl+Enter = " + Msg.getMsg(Env.getCtx(), "Process") + ")");
		m_txbSqlField.setClientAttribute("spellcheck", "false");
		m_txbSqlField.setWidgetListener("onKeyDown",
				"if (event.ctrlKey && event.keyCode == 13) { event.stop(); this.fire('" + EVENT_ON_CTRL_ENTER + "', null, {toServer: true}); }");
		m_txbSqlField.addEventListener(EVENT_ON_CTRL_ENTER, this);
		editorPanel.appendChild(m_txbSqlField);

		north.appendChild(editorPanel);
		north.setSplittable(true);
		north.setCollapsible(true);
		north.setAutoscroll(true);
		ZKUpdateUtil.setHeight(north, "35%");
		layout.appendChild(north);

		// result grid
		center.appendChild(listbox);
		ZKUpdateUtil.setVflex(listbox, "1");
		ZKUpdateUtil.setHflex(listbox, "1");
		listbox.addEventListener(Events.ON_DOUBLE_CLICK, this);
		layout.appendChild(center);

		// status bar
		m_txbResultField.setMultiline(true);
		m_txbResultField.setRows(2);
		ZKUpdateUtil.setHflex(m_txbResultField, "1");
		m_txbResultField.setReadonly(true);
		m_txbResultField.setStyle(STATUS_STYLE);
		south.appendChild(m_txbResultField);
		south.setSplittable(true);
		south.setCollapsible(true);
		ZKUpdateUtil.setHeight(south, "60px");
		layout.appendChild(south);

		this.appendChild(layout);
		this.addEventListener(EVENT_ON_EXECUTE_SQL, this);

		return;
	}

	/**
	 * Create the toolbar with execute/clear/copy/export buttons and the statement history.
	 * @return toolbar
	 */
	private ToolBar createToolbar() {
		ToolBar toolbar = new ToolBar();

		setButtonIcon(m_btnExecute, "z-icon-Process", "images/Process24.png");
		m_btnExecute.setTooltiptext(Msg.getMsg(Env.getCtx(), "Process") + " (Ctrl+Enter)");
		m_btnExecute.addEventListener(Events.ON_CLICK, this);
		toolbar.appendChild(m_btnExecute);

		setButtonIcon(m_btnClear, "z-icon-eraser", "images/Reset24.png");
		m_btnClear.setTooltiptext(Msg.getMsg(Env.getCtx(), "Reset"));
		m_btnClear.addEventListener(Events.ON_CLICK, this);
		toolbar.appendChild(m_btnClear);

		toolbar.appendChild(new Separator("vertical"));

		setButtonIcon(m_btnCopy, "z-icon-copy", "images/Copy24.png");
		m_btnCopy.setTooltiptext(Msg.getMsg(Env.getCtx(), "Copy"));
		m_btnCopy.setDisabled(true);
		m_btnCopy.addEventListener(Events.ON_CLICK, this);
		toolbar.appendChild(m_btnCopy);

		setButtonIcon(m_btnExport, "z-icon-Export", "images/Export24.png");
		m_btnExport.setTooltiptext(Msg.getMsg(Env.getCtx(), "Export") + " (CSV)");
		m_btnExport.setDisabled(true);
		m_btnExport.addEventListener(Events.ON_CLICK, this);
		toolbar.appendChild(m_btnExport);

		toolbar.appendChild(new Separator("vertical"));

		m_cbHistory.setReadonly(true);
		m_cbHistory.setPlaceholder(Msg.getMsg(Env.getCtx(), "History"));
		m_cbHistory.setTooltiptext(Msg.getMsg(Env.getCtx(), "History"));
		ZKUpdateUtil.setWidth(m_cbHistory, "350px");
		m_cbHistory.addEventListener(Events.ON_SELECT, this);
		toolbar.appendChild(m_cbHistory);

		return toolbar;
	}

	/**
	 * Set font icon or theme image on a toolbar button.
	 * @param button
	 * @param iconSclass font icon sclass
	 * @param image theme image path
	 */
	private void setButtonIcon(ToolBarButton button, String iconSclass, String image) {
		if (ThemeManager.isUseFontIconForImage())
			button.setIconSclass(iconSclass);
		else
			button.setImage(ThemeManager.getThemeResource(image));
	}

	/**
	 *  Create Process Button.
	 *  @return button
	 */
	public static final Button createProcessButton() {
		Button btnProcess = new Button();
		if(ThemeManager.isUseFontIconForImage())
			btnProcess.setIconSclass("z-icon-Process");
		else
			btnProcess.setImage(ThemeManager.getThemeResource("images/Process24.png"));
		btnProcess.setName(Msg.getMsg(Env.getCtx(), "Process"));

		return btnProcess;
	}   //  createProcessButton

	/**
	 *  Process SQL Statements.
	 *
	 *  @param sqlStatement a single SQL statement
	 *  @return a string summarizing the results
	 */
	public String processStatement (String sqlStatement) {
		m_txbResultField.setText(null);
		listbox.clear();
		m_header = null;

		if (sqlStatement == null || sqlStatement.length() == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		char[] chars = sqlStatement.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (Character.isWhitespace(c))
				sb.append(' ');
			else
				sb.append(c);
		}
		String sql = sb.toString().trim();
		if (sql.length() == 0)
			return "";
		//
		StringBuilder result = new StringBuilder();
		String SQL = sql.toUpperCase();
		String cleanSQL = SQL
				.replaceAll(REGEX_REMOVE_COMMENTS, "")
				.replaceAll(REGEX_REMOVE_QUOTED_STRINGS, "")
				.replaceFirst(REGEX_REMOVE_LEADING_SPACES, "");

		if (cleanSQL.contains(";")) {
			result.append("ERROR: Multiple Commands Not Allowed");
			return result.toString();
		}

		int timeout = MSysConfig.getIntValue(MSysConfig.FORM_SQL_QUERY_TIMEOUT_IN_SECONDS, 120);
		int maxRecords = MSysConfig.getIntValue(MSysConfig.FORM_SQL_QUERY_MAX_RECORDS, 500);

		String[] allowedKeywords = MSysConfig.getValue(MSysConfig.FORM_SQL_QUERY_ALLOWED_KEYWORDS, "SELECT,WITH,SHOW").split(",");
		boolean isError = true;
		for (int i = 0; i < allowedKeywords.length; i++) {
			if (cleanSQL.startsWith(allowedKeywords[i] + " ")) {
				isError = false;
				break;
			}
		}
		if (isError) {
			result.append("ERROR: Not Allowed Command");
			return result.toString();
		}

		List<String> header = new ArrayList<String>();
		header.add("#");
		model = new ListModelTable();
		Frozen frozen = new Frozen();
		frozen.setColumns(1);
		listbox.appendChild(frozen);

		Trx trx = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			long start = System.currentTimeMillis();

			String trxName = Trx.createTrxName("WSQLQueryEnhanced");
			trx = Trx.get(trxName, false);
			trx.setDisplayName(getClass().getName()+"_processStatement");
			trx.getConnection().setReadOnly(true);

			pstmt = DB.prepareNormalReadReplicaStatement(sql, trxName);
			pstmt.setQueryTimeout(timeout);
			rs = pstmt.executeQuery();

			ResultSetMetaData meta = rs.getMetaData();
			int count = 0;
			for (int col = 1; col <= meta.getColumnCount(); col++) {
				String columnName = meta.getColumnLabel(col);
				header.add(columnName);
			}

			while (rs.next ()) {
				if (count >= maxRecords) {
					result.append("Maximum of " + maxRecords + " records reached.  ");
					break;
				}
				List<Object> row = new ArrayList<Object>();
				row.add(++count);
				for (int col = 1; col <= meta.getColumnCount(); col++) {
					String colName = header.get(col).toLowerCase();
					if (rs.getObject(col) instanceof BigDecimal
						&& (colName.endsWith("_id") || colName.equals("createdby") || colName.equals("updatedby")))
						row.add(rs.getInt(col));
					else
						row.add(rs.getObject(col));
				}	//	for all columns
				model.add(row);
			}
			long end = System.currentTimeMillis();
			BigDecimal durationSeconds = BigDecimal.valueOf(end).subtract(BigDecimal.valueOf(start)).divide(BigDecimal.valueOf(1000.0));
			result.append("Count = ").append(count).append(" - ").append(durationSeconds).append(" s");
			if (MSysConfig.getBooleanValue(MSysConfig.FORM_SQL_QUERY_LOG_ISSUE, true)) {
				MIssue issue = new MIssue(Env.getCtx(), 0, null);
				issue.setIssueSummary("SQL executed on SQL Query form");
				issue.setStackTrace(sql);
				issue.setResponseText(result.toString());
				issue.setIssueSource(MIssue.ISSUESOURCE_Form);
				issue.setUserName(Env.getContext(Env.getCtx(), Env.AD_USER_NAME));
				issue.setAD_Form_ID(SystemIDs.FORM_SQL_QUERY);
				issue.setProcessed(true);
				issue.setComments("Duration : " + durationSeconds + " seconds");
				issue.saveEx();
			}

			// Show the result in WListbox
			WListItemRenderer renderer = new WListItemRenderer(header);
			model.setNoColumns(header.size());
			listbox.setModel(model);
			listbox.setItemRenderer(renderer);
			listbox.initialiseHeader();
			listbox.setSizedByContent(true);
			m_header = header;

		} catch (Exception e) {
			if (trx != null) {
				trx.rollback();
			}
			if (DBException.isTimeout(e)) {
				result.append("Maximum of " + timeout + " seconds reached, query cancelled.");
			} else {
				e.printStackTrace();
				String exception = e.toString();
				log.log(Level.SEVERE, "process statement: " + sql + " - " + exception);
				result.append("Exception => ").append(exception);
			}
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
			if (trx != null) {
				trx.close();
				trx = null;
			}
		}

		return result.toString();
	}

	/**
	 * Show busy indicator and echo {@link #EVENT_ON_EXECUTE_SQL}.
	 */
	private void postExecuteSQLEvent() {
		Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
		Events.echoEvent(EVENT_ON_EXECUTE_SQL, this, null);
	}

	/**
	 * Handle {@link #EVENT_ON_EXECUTE_SQL} event: execute the statement,
	 * update the status bar and the statement history.
	 */
	private void onExecuteSQL() {
		try {
			String sql = m_txbSqlField.getText();
			String result = processStatement(sql);
			boolean isError = result.startsWith("ERROR") || result.contains("Exception =>");
			m_txbResultField.setText(result);
			m_txbResultField.setStyle(isError ? STATUS_STYLE + " color: #B00020; font-weight: bold;" : STATUS_STYLE);
			boolean hasData = !isError && m_header != null && model != null && model.getSize() > 0;
			m_btnCopy.setDisabled(!hasData);
			m_btnExport.setDisabled(!hasData);
			if (!isError && sql != null && sql.trim().length() > 0)
				addToHistory(sql.trim());
		} finally {
			Clients.clearBusy();
		}
	}

	/**
	 * Add a statement to the history (most recent first) and refresh the history combo.
	 * @param sql
	 */
	private void addToHistory(String sql) {
		m_history.remove(sql);
		m_history.add(0, sql);
		while (m_history.size() > MAX_HISTORY)
			m_history.remove(m_history.size() - 1);

		m_cbHistory.getItems().clear();
		for (String statement : m_history) {
			String label = statement.replaceAll("\\s+", " ");
			if (label.length() > 100)
				label = label.substring(0, 100) + "...";
			Comboitem item = m_cbHistory.appendItem(label);
			item.setValue(statement);
			item.setTooltiptext(statement);
		}
	}

	/**
	 * Build a delimited text representation of the current result set,
	 * excluding the row counter column.
	 * @param delimiter column delimiter
	 * @param csv true to quote/escape values as CSV
	 * @return delimited text or null when there is no result set
	 */
	private String buildDelimitedText(String delimiter, boolean csv) {
		if (m_header == null || model == null)
			return null;
		StringBuilder text = new StringBuilder();
		for (int col = 1; col < m_header.size(); col++) {
			if (col > 1)
				text.append(delimiter);
			text.append(formatCell(m_header.get(col), delimiter, csv));
		}
		text.append("\n");
		for (int i = 0; i < model.getSize(); i++) {
			@SuppressWarnings("unchecked")
			List<Object> row = (List<Object>) model.getElementAt(i);
			for (int col = 1; col < row.size(); col++) {
				if (col > 1)
					text.append(delimiter);
				text.append(formatCell(row.get(col), delimiter, csv));
			}
			text.append("\n");
		}
		return text.toString();
	}

	/**
	 * Format a single cell value for {@link #buildDelimitedText(String, boolean)}.
	 * @param value
	 * @param delimiter
	 * @param csv true to quote/escape as CSV, false to strip delimiter characters
	 * @return formatted value
	 */
	private String formatCell(Object value, String delimiter, boolean csv) {
		String cell = value == null ? "" : String.valueOf(value);
		if (csv) {
			if (cell.contains("\"") || cell.contains(",") || cell.contains("\n") || cell.contains("\r"))
				cell = "\"" + cell.replace("\"", "\"\"") + "\"";
		} else {
			cell = cell.replace(delimiter, " ").replace("\r", " ").replace("\n", " ");
		}
		return cell;
	}

	/**
	 * Copy text to the client clipboard and show a notification near the reference component.
	 * @param text
	 * @param ref
	 */
	private void copyToClipboard(String text, Component ref) {
		String base64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
		StringBuilder script = new StringBuilder("navigator.clipboard.writeText(new TextDecoder().decode(Uint8Array.from(atob('")
			.append(base64)
			.append("'), function(c){return c.charCodeAt(0);})));");
		Clients.evalJavaScript(script.toString());
		Notification.show(Msg.getMsg(Env.getCtx(), "Copied"), Notification.TYPE_INFO, ref, "end_before", 1500);
	}

	/**
	 * Copy the full result set to the clipboard as tab separated values.
	 */
	private void copyResultsToClipboard() {
		String text = buildDelimitedText("\t", false);
		if (text != null)
			copyToClipboard(text, m_btnCopy);
	}

	/**
	 * Copy the selected result row to the clipboard as tab separated values.
	 */
	private void copySelectedRowToClipboard() {
		int index = listbox.getSelectedIndex();
		if (index < 0 || model == null || index >= model.getSize())
			return;
		@SuppressWarnings("unchecked")
		List<Object> row = (List<Object>) model.getElementAt(index);
		StringBuilder text = new StringBuilder();
		for (int col = 1; col < row.size(); col++) {
			if (col > 1)
				text.append("\t");
			text.append(formatCell(row.get(col), "\t", false));
		}
		copyToClipboard(text.toString(), listbox);
	}

	/**
	 * Download the current result set as a CSV file.
	 */
	private void exportResults() {
		String csv = buildDelimitedText(",", true);
		if (csv == null)
			return;
		String fileName = "SQLQuery_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
		byte[] content = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);	// BOM so Excel opens the CSV as UTF-8
		Filedownload.save(content, "text/csv", fileName);
	}

    /**
     *  Process the events for this form
     *  @param event
     */
	@Override
	public void onEvent(Event event) throws Exception {
		if (event.getTarget() == m_btnExecute || EVENT_ON_CTRL_ENTER.equals(event.getName()))
			postExecuteSQLEvent();
		else if (EVENT_ON_EXECUTE_SQL.equals(event.getName()))
			onExecuteSQL();
		else if (event.getTarget() == m_btnClear) {
			m_txbSqlField.setText("");
			m_txbSqlField.focus();
		}
		else if (event.getTarget() == m_btnCopy)
			copyResultsToClipboard();
		else if (event.getTarget() == m_btnExport)
			exportResults();
		else if (event.getTarget() == m_cbHistory && Events.ON_SELECT.equals(event.getName())) {
			Comboitem item = m_cbHistory.getSelectedItem();
			if (item != null && item.getValue() != null) {
				m_txbSqlField.setText(item.getValue().toString());
				m_txbSqlField.focus();
			}
		}
		else if (event.getTarget() == listbox && Events.ON_DOUBLE_CLICK.equals(event.getName()))
			copySelectedRowToClipboard();
		super.onEvent(event);
	}
}