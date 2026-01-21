package com.idb.idbonepos.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import com.idb.idbonepos.model.PrinterProfile
import com.idb.idbonepos.model.PrinterProfileType
import com.idb.idbonepos.model.PrinterType
import com.idb.idbonepos.model.PrintSettings

private val Context.settingsDataStore by preferencesDataStore(name = "print_settings")

class SettingsRepository(private val context: Context) {
    private val dataStore = context.settingsDataStore

    val settingsFlow: Flow<PrintSettings> = dataStore.data.map { prefs ->
        val printMode = prefs[KEY_PRINT_MODE]?.let { mode ->
            runCatching { PrintMode.valueOf(mode) }.getOrNull()
        } ?: PrintMode.GRAPHIC

        PrintSettings(
            printerName = prefs[KEY_PRINTER_NAME],
            printerAddress = prefs[KEY_PRINTER_ADDRESS],
            printMode = printMode,
            printWidthMm = prefs[KEY_PRINT_WIDTH] ?: 48,
            printResolutionDpi = prefs[KEY_PRINT_RESOLUTION] ?: 203,
            initialCommands = prefs[KEY_INITIAL_COMMANDS] ?: "",
            cutterCommands = prefs[KEY_CUTTER_COMMANDS] ?: "1D,56,42,00",
            drawerCommands = prefs[KEY_DRAWER_COMMANDS] ?: "1B,70,00,19,FA"
        )
    }

    val printersFlow: Flow<List<PrinterProfile>> = dataStore.data.map { prefs ->
        parsePrinterProfiles(prefs[KEY_PRINTER_PROFILES])
    }

    val selectedPrinterIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_PRINTER_ID]
    }

    suspend fun updatePrinter(name: String, address: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PRINTER_NAME] = name
            prefs[KEY_PRINTER_ADDRESS] = address
        }
    }

    suspend fun upsertPrinter(profile: PrinterProfile, select: Boolean = true) {
        dataStore.edit { prefs ->
            val existing = parsePrinterProfiles(prefs[KEY_PRINTER_PROFILES]).toMutableList()
            val index = existing.indexOfFirst { it.id == profile.id }
            if (index >= 0) {
                existing[index] = profile
            } else {
                existing.add(profile)
            }
            prefs[KEY_PRINTER_PROFILES] = serializePrinterProfiles(existing)
            if (select) {
                prefs[KEY_SELECTED_PRINTER_ID] = profile.id
                prefs[KEY_PRINTER_NAME] = profile.name
                prefs[KEY_PRINTER_ADDRESS] = profile.address
            }
        }
    }

    suspend fun selectPrinter(profile: PrinterProfile) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_PRINTER_ID] = profile.id
            prefs[KEY_PRINTER_NAME] = profile.name
            prefs[KEY_PRINTER_ADDRESS] = profile.address
        }
    }

    suspend fun deletePrinter(printerId: String) {
        dataStore.edit { prefs ->
            val existing = parsePrinterProfiles(prefs[KEY_PRINTER_PROFILES]).toMutableList()
            val removed = existing.firstOrNull { it.id == printerId }
            existing.removeAll { it.id == printerId }
            prefs[KEY_PRINTER_PROFILES] = serializePrinterProfiles(existing)

            val newSelected = existing.firstOrNull()
            if (newSelected == null) {
                prefs.remove(KEY_SELECTED_PRINTER_ID)
                prefs.remove(KEY_PRINTER_NAME)
                prefs.remove(KEY_PRINTER_ADDRESS)
            } else {
                prefs[KEY_SELECTED_PRINTER_ID] = newSelected.id
                prefs[KEY_PRINTER_NAME] = newSelected.name
                prefs[KEY_PRINTER_ADDRESS] = newSelected.address
            }

            val currentEthernetIp = prefs[KEY_ETHERNET_IP] ?: ""
            if (removed?.type == PrinterType.ETHERNET && removed.address == currentEthernetIp) {
                prefs[KEY_ETHERNET_IP] = ""
            }
        }
    }

    suspend fun updatePrintMode(mode: PrintMode) {
        dataStore.edit { prefs ->
            prefs[KEY_PRINT_MODE] = mode.name
        }
    }

    suspend fun updatePrintWidth(widthMm: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_PRINT_WIDTH] = widthMm
        }
    }

    suspend fun updatePrintResolution(resolutionDpi: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_PRINT_RESOLUTION] = resolutionDpi
        }
    }

    suspend fun updateInitialCommands(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_INITIAL_COMMANDS] = value
        }
    }

    suspend fun updateCutterCommands(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CUTTER_COMMANDS] = value
        }
    }

    suspend fun updateDrawerCommands(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_DRAWER_COMMANDS] = value
        }
    }

    suspend fun saveEthernetPrinterIP(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ETHERNET_IP] = value
        }
    }

    suspend fun savePdfUrl(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PDF_URL] = value
        }
    }

    suspend fun saveWebUrl(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_WEB_URL] = value
        }
    }

    suspend fun getEthernetPrinterIP(): String {
        return dataStore.data.map { prefs -> prefs[KEY_ETHERNET_IP] ?: "" }.first()
    }

    suspend fun getPdfUrl(): String {
        return dataStore.data.map { prefs -> prefs[KEY_PDF_URL] ?: "" }.first()
    }

    suspend fun getWebUrl(): String {
        return dataStore.data.map { prefs -> prefs[KEY_WEB_URL] ?: "" }.first()
    }

    private fun parsePrinterProfiles(raw: String?): List<PrinterProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val items = mutableListOf<PrinterProfile>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val type = runCatching {
                    PrinterType.valueOf(obj.optString("type"))
                }.getOrNull() ?: PrinterType.BLUETOOTH
                val profileType = runCatching {
                    PrinterProfileType.valueOf(obj.optString("profileType"))
                }.getOrNull() ?: PrinterProfileType.ORDER
                val mode = runCatching {
                    PrintMode.valueOf(obj.optString("printMode"))
                }.getOrNull() ?: PrintMode.GRAPHIC
                items.add(
                    PrinterProfile(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        address = obj.optString("address"),
                        type = type,
                        printMode = mode,
                        printWidthMm = obj.optInt("printWidthMm", 48),
                        printResolutionDpi = obj.optInt("printResolutionDpi", 203),
                        initialCommands = obj.optString("initialCommands"),
                        cutterCommands = obj.optString("cutterCommands", "1D,56,42,00"),
                        drawerCommands = obj.optString("drawerCommands", "1B,70,00,19,FA"),
                        graphicTestUrl = obj.optString("graphicTestUrl"),
                        profileType = profileType
                    )
                )
            }
            items
        }.getOrDefault(emptyList())
    }

    private fun serializePrinterProfiles(items: List<PrinterProfile>): String {
        val array = JSONArray()
        items.forEach { profile ->
            val obj = JSONObject()
            obj.put("id", profile.id)
            obj.put("name", profile.name)
            obj.put("address", profile.address)
            obj.put("type", profile.type.name)
            obj.put("printMode", profile.printMode.name)
            obj.put("printWidthMm", profile.printWidthMm)
            obj.put("printResolutionDpi", profile.printResolutionDpi)
            obj.put("initialCommands", profile.initialCommands)
            obj.put("cutterCommands", profile.cutterCommands)
            obj.put("drawerCommands", profile.drawerCommands)
            obj.put("graphicTestUrl", profile.graphicTestUrl)
            obj.put("profileType", profile.profileType.name)
            array.put(obj)
        }
        return array.toString()
    }

    companion object {
        private val KEY_PRINTER_NAME = stringPreferencesKey("printer_name")
        private val KEY_PRINTER_ADDRESS = stringPreferencesKey("printer_address")
        private val KEY_SELECTED_PRINTER_ID = stringPreferencesKey("selected_printer_id")
        private val KEY_PRINTER_PROFILES = stringPreferencesKey("printer_profiles")
        private val KEY_PRINT_MODE = stringPreferencesKey("print_mode")
        private val KEY_PRINT_WIDTH = intPreferencesKey("print_width_mm")
        private val KEY_PRINT_RESOLUTION = intPreferencesKey("print_resolution_dpi")
        private val KEY_INITIAL_COMMANDS = stringPreferencesKey("initial_commands")
        private val KEY_CUTTER_COMMANDS = stringPreferencesKey("cutter_commands")
        private val KEY_DRAWER_COMMANDS = stringPreferencesKey("drawer_commands")
        private val KEY_ETHERNET_IP = stringPreferencesKey("ethernet_printer_ip")
        private val KEY_PDF_URL = stringPreferencesKey("pdf_url")
        private val KEY_WEB_URL = stringPreferencesKey("web_url")
    }
}
