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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.exceptions.DBException;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListCell;
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
import org.zkoss.zul.Listcell;
import org.zkoss.zul.North;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Separator;
import org.zkoss.zul.South;
import org.zkoss.zul.Vlayout;

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
	/** Echo event to bind the client script once the form is attached (final component uuids). */
	private static final String EVENT_ON_INIT_CLIENT = "onInitClient";
	/** Client side event requesting the bundled CodeMirror library (once per page). */
	private static final String EVENT_ON_LOAD_EDITOR_LIB = "onLoadEditorLib";
	/** Client side event requesting an execution (Ctrl+Enter or execute button). */
	private static final String EVENT_ON_CLIENT_EXECUTE = "onClientExecute";
	/** Key of the editor selection in the {@link #EVENT_ON_CLIENT_EXECUTE} data. */
	private static final String EVENT_DATA_SELECTION = "sel";
	/** Maximum number of statements kept in the history. */
	private static final int MAX_HISTORY = 50;
	/** Issue summary used to log executed statements, also used to reload the history. */
	private static final String ISSUE_SUMMARY = "SQL executed on SQL Query form";
	/** SysConfig to use the CodeMirror code editor (Y) or the plain textarea (N). */
	private static final String SYSCONFIG_CODE_EDITOR = "SQLQUERY_ENHANCED_CODE_EDITOR";
	/** Client side script, resource next to this class. */
	private static final String CLIENT_SCRIPT = "WSQLQueryEnhanced.js";
	/** Bundled CodeMirror scripts, in load order, resources next to this class. */
	private static final String[] CODEMIRROR_SCRIPTS = {"codemirror/codemirror.min.js", "codemirror/sql.min.js", "codemirror/placeholder.min.js"};
	/** Bundled CodeMirror stylesheet. */
	private static final String CODEMIRROR_CSS = "codemirror/codemirror.min.css";
	/** Text shown for null values in the result grid. */
	private static final String NULL_DISPLAY = "NULL";
	/** Style of null values in the result grid. */
	private static final String NULL_STYLE = "color: #9E9E9E; font-style: italic;";
	/** Base style of the error field. */
	private static final String ERROR_STYLE = "font-family: monospace; font-size: 12px; border: none; background-color: transparent; color: #B00020;";
	/** Base style of the status label in the toolbar. */
	private static final String STATUS_STYLE = "margin-left: 12px; white-space: nowrap; font-family: monospace; font-size: 12px;";
	/** Message key and default text of the shortcuts help. */
	private static final String MSG_HELP = "SQLQueryEnhancedHelp";
	private static final String MSG_HELP_DEFAULT = "Ctrl+Enter: execute (only the selected text, if any)\n"
			+ "Tab / Shift+Tab: indent / unindent\n"
			+ "Double click on a row: copy the row\n"
			+ "Copy and Export use raw values, without display format";

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
	/** Keyboard shortcuts help button. */
	private ToolBarButton m_btnHelp = new ToolBarButton();
	/** Previously executed statements. */
	private Combobox m_cbHistory = new Combobox();
	/** Result summary of the last execution. */
	private Label m_lblStatus = new Label();
	/** Error region, only visible when the last execution failed. */
	private South m_southError = new South();
	/** Field to hold the error of the last execution. */
	private Textbox m_txbResultField = new Textbox();
	/** Grid to show the result data. */
	private ListModelTable model = null;
	/** Column headers of the last successful execution (first entry is the row counter). */
	private List<String> m_header = null;
	/** Number of rows of the last execution. */
	private int m_rowCount = 0;
	/** True when the last execution hit the maximum number of records. */
	private boolean m_isMaxReached = false;
	private WListbox listbox = new WListbox();
	/** Statement history, most recent first. */
	private List<HistoryEntry> m_history = new ArrayList<HistoryEntry>();
	/** Client script, loaded once. */
	private static String s_clientScript = null;
	/** CodeMirror library script, loaded once. */
	private static String s_editorLibScript = null;

	/**
	 * Remove comment
	 */
	private static final String REGEX_REMOVE_COMMENTS = "/\\*(?:.|[\\n\\r])*?\\*/";

	/**
	 * Remove quoted string
	 */
	private static final String REGEX_REMOVE_QUOTED_STRINGS = "'(?:.|[\\n\\r])*?'";

	/**
	 * Remove line comment
	 */
	private static final String REGEX_REMOVE_LINE_COMMENTS = "--[^\\n\\r]*";

	/**
	 * Remove trailing semicolons
	 */
	private static final String REGEX_REMOVE_TRAILING_SEMICOLONS = "[;\\s]+$";

	/**
	 * Row count in the result text logged in the issue
	 */
	private static final Pattern PATTERN_COUNT = Pattern.compile("Count = (\\d+)");

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
		m_txbSqlField.setReadonly(false);
		m_txbSqlField.setStyle("width: 100%; height: 100%; box-sizing: border-box; font-family: monospace; font-size: 13px; resize: none;");
		m_txbSqlField.setPlaceholder("SELECT ...   (Ctrl+Enter = " + Msg.getMsg(Env.getCtx(), "QueryExecute") + ")");
		m_txbSqlField.setClientAttribute("spellcheck", "false");
		m_txbSqlField.addEventListener(EVENT_ON_CLIENT_EXECUTE, this);
		m_txbSqlField.addEventListener(EVENT_ON_LOAD_EDITOR_LIB, this);
		// the holder gets the remaining height, the editor (textarea or CodeMirror) fills it
		Div editorHolder = new Div();
		ZKUpdateUtil.setHflex(editorHolder, "1");
		ZKUpdateUtil.setVflex(editorHolder, "1");
		editorHolder.appendChild(m_txbSqlField);
		editorPanel.appendChild(editorHolder);

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

		// error region
		m_txbResultField.setMultiline(true);
		ZKUpdateUtil.setHflex(m_txbResultField, "1");
		ZKUpdateUtil.setVflex(m_txbResultField, "1");
		m_txbResultField.setReadonly(true);
		m_txbResultField.setStyle(ERROR_STYLE);
		m_southError.appendChild(m_txbResultField);
		m_southError.setSplittable(true);
		ZKUpdateUtil.setHeight(m_southError, "80px");
		m_southError.setVisible(false);
		layout.appendChild(m_southError);

		this.appendChild(layout);
		this.addEventListener(EVENT_ON_EXECUTE_SQL, this);
		this.addEventListener(EVENT_ON_INIT_CLIENT, this);

		loadHistory();
		// uuids are temporary until the form is attached to the page
		Events.echoEvent(EVENT_ON_INIT_CLIENT, this, null);
		return;
	}

	/**
	 * Create the toolbar with execute/clear/copy/export buttons, the statement history and the status.
	 * @return toolbar
	 */
	private ToolBar createToolbar() {
		ToolBar toolbar = new ToolBar();

		setButtonIcon(m_btnExecute, "z-icon-Process", "images/Process24.png");
		m_btnExecute.setLabel(Msg.getMsg(Env.getCtx(), "QueryExecute"));
		m_btnExecute.setTooltiptext(Msg.getMsg(Env.getCtx(), "QueryExecute") + " (Ctrl+Enter)");
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

		// not readonly: iDempiere's ValidateReadonlyComponent drops onSelect/onChange of readonly comboboxes
		m_cbHistory.setAutodrop(true);
		m_cbHistory.setPlaceholder(Msg.getMsg(Env.getCtx(), "History"));
		m_cbHistory.setTooltiptext(Msg.getMsg(Env.getCtx(), "History"));
		ZKUpdateUtil.setWidth(m_cbHistory, "350px");
		m_cbHistory.addEventListener(Events.ON_SELECT, this);
		toolbar.appendChild(m_cbHistory);

		// shortcuts help, opened on the client without a server round trip
		Popup helpPopup = new Popup();
		Vlayout helpLines = new Vlayout();
		helpLines.setStyle("padding: 6px 10px;");
		for (String line : getMsg(MSG_HELP, MSG_HELP_DEFAULT).split("\n"))
			helpLines.appendChild(new Label(line));
		helpPopup.appendChild(helpLines);
		toolbar.appendChild(helpPopup);
		setButtonIcon(m_btnHelp, "z-icon-Help", "images/Help24.png");
		m_btnHelp.setTooltiptext(Msg.getMsg(Env.getCtx(), "Help"));
		m_btnHelp.setPopupAttributes(helpPopup, "after_start", null, null, null);
		toolbar.appendChild(m_btnHelp);

		m_lblStatus.setStyle(STATUS_STYLE);
		toolbar.appendChild(m_lblStatus);

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
	 * Get a translated message, falling back to a default text when the message is not defined.
	 * @param key AD_Message value
	 * @param defaultText text used when the message does not exist
	 * @return message text
	 */
	private String getMsg(String key, String defaultText) {
		String text = Msg.getMsg(Env.getCtx(), key);
		return text == null || text.equals(key) ? defaultText : text;
	}

	/**
	 * Send the client script and bind it to the editor and the execute button.
	 * The script upgrades the editor to CodeMirror unless disabled by SysConfig.
	 */
	private void initClientScript() {
		String script = getClientScript();
		if (script == null)
			return;
		boolean isCodeEditor = MSysConfig.getBooleanValue(SYSCONFIG_CODE_EDITOR, true);
		String mode = DB.isOracle() ? "text/x-plsql" : "text/x-pgsql";
		StringBuilder init = new StringBuilder(script)
			.append("\nSQLQueryEnhanced.init({")
			.append("editor:'").append(m_txbSqlField.getUuid()).append("',")
			.append("button:'").append(m_btnExecute.getUuid()).append("',")
			.append("event:'").append(EVENT_ON_CLIENT_EXECUTE).append("',")
			.append("key:'").append(EVENT_DATA_SELECTION).append("',")
			.append("codeEditor:").append(isCodeEditor).append(",")
			.append("libEvent:'").append(EVENT_ON_LOAD_EDITOR_LIB).append("',")
			.append("mode:'").append(mode).append("'});");
		Clients.evalJavaScript(init.toString());
	}

	/**
	 * Send the bundled CodeMirror library to the client, requested once per page by the client script.
	 */
	private void sendEditorLib() {
		String script = getEditorLibScript();
		if (script != null)
			Clients.evalJavaScript(script);
	}

	/**
	 * @return client script source, or null when it can not be read
	 */
	private static synchronized String getClientScript() {
		if (s_clientScript == null)
			s_clientScript = readResource(CLIENT_SCRIPT);
		return s_clientScript;
	}

	/**
	 * @return CodeMirror scripts followed by the call that applies its stylesheet and attaches
	 * the waiting editors, or null when a resource can not be read
	 */
	private static synchronized String getEditorLibScript() {
		if (s_editorLibScript == null) {
			StringBuilder script = new StringBuilder();
			for (String name : CODEMIRROR_SCRIPTS) {
				String source = readResource(name);
				if (source == null)
					return null;
				script.append(source).append("\n;\n");
			}
			String css = readResource(CODEMIRROR_CSS);
			if (css == null)
				return null;
			script.append("SQLQueryEnhanced.libLoaded('").append(escapeJavaScript(css)).append("');");
			s_editorLibScript = script.toString();
		}
		return s_editorLibScript;
	}

	/**
	 * @param name resource name relative to this class
	 * @return resource content, or null when it can not be read
	 */
	private static String readResource(String name) {
		try (InputStream in = WSQLQueryEnhanced.class.getResourceAsStream(name)) {
			if (in != null)
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			log.warning("Resource not found: " + name);
		} catch (IOException e) {
			log.log(Level.WARNING, "Can't read resource " + name, e);
		}
		return null;
	}

	/**
	 * @param text
	 * @return text escaped to be used inside a single quoted JavaScript string
	 */
	private static String escapeJavaScript(String text) {
		return text.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r").replace("\n", "\\n")
				.replace("</", "<\\/");
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
		m_rowCount = 0;
		m_isMaxReached = false;

		if (sqlStatement == null || sqlStatement.length() == 0)
			return "";
		// line breaks are kept so that "--" comments only cover their own line
		String sql = sqlStatement.replaceFirst(REGEX_REMOVE_TRAILING_SEMICOLONS, "").trim();
		if (sql.length() == 0)
			return "";
		//
		StringBuilder result = new StringBuilder();
		String cleanSQL = sql.toUpperCase()
				.replaceAll(REGEX_REMOVE_COMMENTS, " ")
				.replaceAll(REGEX_REMOVE_QUOTED_STRINGS, "''")
				.replaceAll(REGEX_REMOVE_LINE_COMMENTS, " ")
				.replaceAll("\\s+", " ")
				.trim();

		if (cleanSQL.contains(";")) {
			result.append("ERROR: Multiple Commands Not Allowed");
			return result.toString();
		}

		int timeout = MSysConfig.getIntValue(MSysConfig.FORM_SQL_QUERY_TIMEOUT_IN_SECONDS, 120);
		int maxRecords = MSysConfig.getIntValue(MSysConfig.FORM_SQL_QUERY_MAX_RECORDS, 500);

		String[] allowedKeywords = MSysConfig.getValue(MSysConfig.FORM_SQL_QUERY_ALLOWED_KEYWORDS, "SELECT,WITH,SHOW").split(",");
		boolean isError = true;
		for (int i = 0; i < allowedKeywords.length; i++) {
			String keyword = allowedKeywords[i].trim().toUpperCase();
			if (keyword.length() > 0 && cleanSQL.matches(Pattern.quote(keyword) + "\\b.*")) {
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
			boolean[] isIntegerColumn = new boolean[meta.getColumnCount() + 1];
			for (int col = 1; col <= meta.getColumnCount(); col++) {
				String columnName = meta.getColumnLabel(col);
				header.add(columnName);
				String colName = columnName.toLowerCase();
				// ids and numeric columns without decimals are shown as integers
				isIntegerColumn[col] = colName.endsWith("_id") || colName.equals("createdby") || colName.equals("updatedby")
					|| ((meta.getColumnType(col) == Types.NUMERIC || meta.getColumnType(col) == Types.DECIMAL)
						&& meta.getScale(col) == 0 && meta.getPrecision(col) > 0);
			}

			while (rs.next ()) {
				if (count >= maxRecords) {
					result.append("Maximum of " + maxRecords + " records reached.  ");
					m_isMaxReached = true;
					break;
				}
				List<Object> row = new ArrayList<Object>();
				row.add(++count);
				for (int col = 1; col <= meta.getColumnCount(); col++) {
					Object value = rs.getObject(col);
					if (value instanceof BigDecimal && isIntegerColumn[col])
						row.add(toInteger((BigDecimal) value));
					else
						row.add(value);
				}	//	for all columns
				model.add(row);
			}
			m_rowCount = count;
			long end = System.currentTimeMillis();
			BigDecimal durationSeconds = BigDecimal.valueOf(end).subtract(BigDecimal.valueOf(start)).divide(BigDecimal.valueOf(1000.0));
			result.append("Count = ").append(count).append(" - ").append(durationSeconds).append(" s");
			if (MSysConfig.getBooleanValue(MSysConfig.FORM_SQL_QUERY_LOG_ISSUE, true)) {
				MIssue issue = new MIssue(Env.getCtx(), 0, null);
				issue.setIssueSummary(ISSUE_SUMMARY);
				issue.setStackTrace(sql);
				issue.setResponseText(result.toString());
				issue.setIssueSource(MIssue.ISSUESOURCE_Form);
				issue.setUserName(Env.getContext(Env.getCtx(), Env.AD_USER_NAME));
				issue.setAD_Form_ID(getAdFormId());
				issue.setProcessed(true);
				issue.setComments("Duration : " + durationSeconds + " seconds");
				issue.saveEx();
			}

			// Show the result in WListbox
			WListItemRenderer renderer = new ResultRenderer(header);
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
				log.log(Level.SEVERE, "process statement: " + sql, e);
				result.append("Exception => ").append(e.toString());
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
	 * @param value numeric value without decimals
	 * @return Integer when it fits, Long otherwise
	 */
	private static Number toInteger(BigDecimal value) {
		long longValue = value.longValue();
		if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE)
			return Integer.valueOf((int) longValue);
		return Long.valueOf(longValue);
	}

	/**
	 * Show busy indicator and echo {@link #EVENT_ON_EXECUTE_SQL}.
	 * @param sql statement to execute, null to execute the whole editor content
	 */
	private void postExecuteSQLEvent(String sql) {
		Clients.showBusy(Msg.getMsg(Env.getCtx(), "Processing"));
		Events.echoEvent(EVENT_ON_EXECUTE_SQL, this, sql);
	}

	/**
	 * Handle {@link #EVENT_ON_EXECUTE_SQL} event: execute the statement,
	 * update the status and the statement history.
	 * @param selection selected text to execute, null or empty to execute the whole editor content
	 */
	private void onExecuteSQL(String selection) {
		try {
			boolean isSelection = selection != null && selection.trim().length() > 0;
			String sql = isSelection ? selection : m_txbSqlField.getText();
			String result = processStatement(sql);
			boolean isError = result.startsWith("ERROR") || result.contains("Exception =>");
			if (isSelection && !isError)
				result = result + " (" + Msg.getMsg(Env.getCtx(), "Selection") + ")";
			showStatus(result, isError);
			boolean hasData = !isError && m_header != null && model != null && model.getSize() > 0;
			m_btnCopy.setDisabled(!hasData);
			m_btnExport.setDisabled(!hasData);
			if (!isError && sql != null && sql.trim().length() > 0)
				addToHistory(new HistoryEntry(sql.trim(), new Timestamp(System.currentTimeMillis()), m_rowCount));
		} finally {
			Clients.clearBusy();
		}
	}

	/**
	 * Show the result summary in the toolbar; errors are also shown in full in the error region.
	 * @param result result text of {@link #processStatement(String)}
	 * @param isError
	 */
	private void showStatus(String result, boolean isError) {
		if (isError) {
			String error = Msg.getMsg(Env.getCtx(), "Error").trim();
			m_lblStatus.setValue(error.endsWith(":") ? error.substring(0, error.length() - 1) : error);
			m_lblStatus.setStyle(STATUS_STYLE + " color: #B00020; font-weight: bold;");
			m_txbResultField.setText(result);
		} else {
			m_lblStatus.setValue(result);
			m_lblStatus.setStyle(m_isMaxReached ? STATUS_STYLE + " color: #B26A00; font-weight: bold;" : STATUS_STYLE);
			m_txbResultField.setText(null);
		}
		m_lblStatus.setTooltiptext(result);
		m_southError.setVisible(isError);
	}

	/**
	 * Load the statements previously executed by the user from the issues logged by this form.
	 */
	private void loadHistory() {
		if (!MSysConfig.getBooleanValue(MSysConfig.FORM_SQL_QUERY_LOG_ISSUE, true))
			return;
		String sql = "SELECT StackTrace, Created, ResponseText FROM AD_Issue"
				+ " WHERE AD_Form_ID=? AND CreatedBy=? AND IssueSummary=?"
				+ " ORDER BY Created DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, getAdFormId());
			pstmt.setInt(2, Env.getAD_User_ID(Env.getCtx()));
			pstmt.setString(3, ISSUE_SUMMARY);
			pstmt.setMaxRows(MAX_HISTORY * 4);	// repeated statements are merged
			rs = pstmt.executeQuery();
			while (rs.next() && m_history.size() < MAX_HISTORY) {
				String statement = rs.getString(1);
				if (statement == null || statement.trim().length() == 0)
					continue;
				statement = statement.trim();
				boolean isRepeated = false;
				for (HistoryEntry entry : m_history) {
					if (entry.sql.equals(statement)) {
						isRepeated = true;
						break;
					}
				}
				if (isRepeated)
					continue;
				int rows = -1;
				Matcher matcher = PATTERN_COUNT.matcher(rs.getString(3) != null ? rs.getString(3) : "");
				if (matcher.find())
					rows = Integer.parseInt(matcher.group(1));
				m_history.add(new HistoryEntry(statement, rs.getTimestamp(2), rows));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't load history", e);
		} finally {
			DB.close(rs, pstmt);
		}
		refreshHistoryCombo();
	}

	/**
	 * Add a statement to the history (most recent first) and refresh the history combo.
	 * @param entry
	 */
	private void addToHistory(HistoryEntry entry) {
		m_history.removeIf(e -> e.sql.equals(entry.sql));
		m_history.add(0, entry);
		while (m_history.size() > MAX_HISTORY)
			m_history.remove(m_history.size() - 1);
		refreshHistoryCombo();
	}

	/**
	 * Rebuild the history combo items from {@link #m_history}.
	 */
	private void refreshHistoryCombo() {
		m_cbHistory.getItems().clear();
		String records = Msg.getMsg(Env.getCtx(), "Records").toLowerCase();
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM HH:mm");
		LocalDate today = LocalDate.now();
		for (HistoryEntry entry : m_history) {
			StringBuilder label = new StringBuilder();
			if (entry.executed != null) {
				boolean isToday = entry.executed.toLocalDateTime().toLocalDate().equals(today);
				label.append((isToday ? timeFormat : dateTimeFormat).format(entry.executed)).append(" · ");
			}
			if (entry.rows >= 0)
				label.append(entry.rows).append(" ").append(records).append(" · ");
			String statement = entry.sql.replaceAll("\\s+", " ");
			if (statement.length() > 100)
				statement = statement.substring(0, 100) + "...";
			label.append(statement);
			Comboitem item = m_cbHistory.appendItem(label.toString());
			item.setValue(entry.sql);
			item.setTooltiptext(entry.sql);
		}
	}

	/**
	 * Load the selected history statement into the editor and reset the combo,
	 * so picking the same statement again still fires a selection event.
	 */
	private void onHistorySelected() {
		Comboitem item = m_cbHistory.getSelectedItem();
		if (item == null || item.getValue() == null)
			return;
		m_txbSqlField.setText(item.getValue().toString());
		m_cbHistory.setSelectedItem(null);
		m_cbHistory.setText("");
		m_cbHistory.clearLastSel();
		m_txbSqlField.focus();
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
		byte[] content = ("﻿" + csv).getBytes(StandardCharsets.UTF_8);	// BOM so Excel opens the CSV as UTF-8
		Filedownload.save(content, "text/csv", fileName);
	}

	/**
	 * Get the editor selection sent by the client with {@link #EVENT_ON_CLIENT_EXECUTE}.
	 * @param event
	 * @return selected text or null
	 */
	private String getSelection(Event event) {
		if (event.getData() instanceof Map) {
			Object sel = ((Map<?, ?>) event.getData()).get(EVENT_DATA_SELECTION);
			if (sel != null && sel.toString().trim().length() > 0)
				return sel.toString();
		}
		return null;
	}

    /**
     *  Process the events for this form
     *  @param event
     */
	@Override
	public void onEvent(Event event) throws Exception {
		if (event.getTarget() == m_btnExecute)
			postExecuteSQLEvent(null);
		else if (EVENT_ON_CLIENT_EXECUTE.equals(event.getName()))
			postExecuteSQLEvent(getSelection(event));
		else if (EVENT_ON_EXECUTE_SQL.equals(event.getName()))
			onExecuteSQL((String) event.getData());
		else if (EVENT_ON_INIT_CLIENT.equals(event.getName()))
			initClientScript();
		else if (EVENT_ON_LOAD_EDITOR_LIB.equals(event.getName()))
			sendEditorLib();
		else if (event.getTarget() == m_btnClear) {
			m_txbSqlField.setText("");
			m_txbSqlField.focus();
		}
		else if (event.getTarget() == m_btnCopy)
			copyResultsToClipboard();
		else if (event.getTarget() == m_btnExport)
			exportResults();
		else if (event.getTarget() == m_cbHistory && Events.ON_SELECT.equals(event.getName()))
			onHistorySelected();
		else if (event.getTarget() == listbox && Events.ON_DOUBLE_CLICK.equals(event.getName()))
			copySelectedRowToClipboard();
		super.onEvent(event);
	}

	/**
	 * Statement of the history.
	 */
	private static class HistoryEntry implements Serializable {
		private static final long serialVersionUID = 1L;
		/** Executed statement. */
		private final String sql;
		/** Execution time, null if unknown. */
		private final Timestamp executed;
		/** Number of rows, -1 if unknown. */
		private final int rows;

		/**
		 * @param sql
		 * @param executed
		 * @param rows
		 */
		private HistoryEntry(String sql, Timestamp executed, int rows) {
			this.sql = sql;
			this.executed = executed;
			this.rows = rows;
		}
	}

	/**
	 * Result grid renderer showing null values as {@link #NULL_DISPLAY}.
	 */
	private static class ResultRenderer extends WListItemRenderer {
		/**
		 * @param header column names
		 */
		private ResultRenderer(List<String> header) {
			super(header);
		}

		@Override
		protected Listcell getCellComponent(WListbox table, Object value, int rowIndex, int columnIndex) {
			if (value != null)
				return super.getCellComponent(table, value, rowIndex, columnIndex);
			ListCell cell = new ListCell(NULL_DISPLAY);
			cell.setStyle(NULL_STYLE);
			return cell;
		}
	}
}
