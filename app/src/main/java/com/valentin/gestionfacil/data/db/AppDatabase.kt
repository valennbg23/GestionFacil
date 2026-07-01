package com.valentin.gestionfacil.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.valentin.gestionfacil.data.db.converter.Converters
import com.valentin.gestionfacil.data.db.dao.CategoriaDao
import com.valentin.gestionfacil.data.db.dao.MetaDao
import com.valentin.gestionfacil.data.db.dao.MovimientoDao
import com.valentin.gestionfacil.data.db.dao.PresupuestoDao
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.MovimientoMeta
import com.valentin.gestionfacil.data.entity.Presupuesto

@Database(
    entities = [
        Movimiento::class,
        Categoria::class,
        Presupuesto::class,
        Meta::class,
        MovimientoMeta::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movimientoDao(): MovimientoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun metaDao(): MetaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Migración v1 -> v2:
         * Crea las nuevas tablas de metas y movimientos_meta SIN borrar
         * los datos existentes (categorías, movimientos, presupuestos).
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabla metas
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        icono TEXT NOT NULL,
                        color TEXT NOT NULL,
                        objetivo REAL NOT NULL,
                        ahorrado REAL NOT NULL,
                        fechaLimite TEXT,
                        fechaCreacion TEXT NOT NULL
                    )
                """.trimIndent())

                // Tabla movimientos_meta
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS movimientos_meta (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        metaId INTEGER NOT NULL,
                        importe REAL NOT NULL,
                        tipo TEXT NOT NULL,
                        fecha TEXT NOT NULL,
                        nota TEXT NOT NULL,
                        FOREIGN KEY(metaId) REFERENCES metas(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Índices de movimientos_meta
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_meta_metaId ON movimientos_meta(metaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_meta_fecha ON movimientos_meta(fecha)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestionfacil.db"
                )
                    .addCallback(SeedCallback())
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    /**
     * Inserta las categorías predefinidas la primera vez que se crea la BD.
     */
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CATEGORIAS_PREDEFINIDAS.forEach { cat ->
                db.execSQL(
                    """
                    INSERT INTO categorias (nombre, icono, color, esPredefinida)
                    VALUES (?, ?, ?, 1)
                    """.trimIndent(),
                    arrayOf<Any>(cat.nombre, cat.icono, cat.color)
                )
            }
        }
    }
}

val CATEGORIAS_PREDEFINIDAS: List<Categoria> = listOf(
    Categoria(nombre = "Comida y restaurantes",   icono = "Restaurant",    color = "#EF5350", esPredefinida = true),
    Categoria(nombre = "Supermercado",            icono = "ShoppingCart",  color = "#FF7043", esPredefinida = true),
    Categoria(nombre = "Transporte",              icono = "DirectionsCar", color = "#42A5F5", esPredefinida = true),
    Categoria(nombre = "Ocio y entretenimiento",  icono = "SportsEsports", color = "#AB47BC", esPredefinida = true),
    Categoria(nombre = "Hogar y facturas",        icono = "Home",          color = "#26A69A", esPredefinida = true),
    Categoria(nombre = "Salud y farmacia",        icono = "LocalHospital", color = "#66BB6A", esPredefinida = true),
    Categoria(nombre = "Ropa y calzado",          icono = "Checkroom",     color = "#EC407A", esPredefinida = true),
    Categoria(nombre = "Educación",               icono = "School",        color = "#7E57C2", esPredefinida = true),
    Categoria(nombre = "Viajes",                  icono = "Flight",        color = "#29B6F6", esPredefinida = true),
    Categoria(nombre = "Suscripciones",           icono = "Subscriptions", color = "#FFA726", esPredefinida = true),
    Categoria(nombre = "Otros gastos",            icono = "MoreHoriz",     color = "#78909C", esPredefinida = true),
    Categoria(nombre = "Nómina / Salario",        icono = "Payments",      color = "#43A047", esPredefinida = true),
    Categoria(nombre = "Trabajo freelance",       icono = "Work",          color = "#00897B", esPredefinida = true),
    Categoria(nombre = "Otros ingresos",          icono = "AddCircle",     color = "#5C6BC0", esPredefinida = true),
)