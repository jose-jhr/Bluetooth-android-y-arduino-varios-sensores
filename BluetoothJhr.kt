package com.ingenieriajhr.variasactivitys


import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class BluetoothJhr(
){


    companion object{


        /*
  Permisos manifiest
  <uses-permission android:name="android.permission.BLUETOOTH"/>
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
   */

        val TAG = "Dispositivos"

        lateinit var mBtAdapter: BluetoothAdapter

        var DEVICE_ADDRES = "dispAddres"

        lateinit var mDevicesArrayAdapter:ArrayAdapter<String>

        lateinit var toastEdit:Toast

        var address :String = ""

        lateinit var dispEmparejadosLista: MutableSet<BluetoothDevice>

        var activityAnterior1 :Class<*>? = null

        lateinit var ctx :Context

        lateinit var listView:ListView

        var mensaje = ""

        lateinit var segundaActivity:Class<*>

        var inicioConexion = false


        fun parameters(ctx1: Context,listView: ListView?,segundaActivity: Class<*>,ctx2:Context,activityAnterior: Class<*>?){
            this.ctx = ctx1
            this.ctx2 = ctx2
            this.listView = listView!!
            this.segundaActivity = segundaActivity
            this.activityAnterior1 = activityAnterior
        }

        /**
         *Iniciamos el bluetooth
         */
        fun onBluetooth (){
            //cargarmos adapter para saber si el dispositivo
            //tiene bluetooth y si este esta encendido
            mBtAdapter = BluetoothAdapter.getDefaultAdapter()

            if (mBtAdapter == null){
                Toast.makeText(ctx,"Dispositivo no soporta bluetooth",Toast.LENGTH_LONG).show()
            }else{
                if (mBtAdapter.isEnabled){
                }else{
                    val enableBlue = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    ctx.startActivity(enableBlue)
                }
            }
            /**
             *Opcion A) se envia a esta funcion un listView
             * para asi poder conectarse
             */
            if (listView!=null){
                dispEmparejadosLista = mBtAdapter.bondedDevices
                mDevicesArrayAdapter = ArrayAdapter(ctx,R.layout.device_name)
                viewDips(mBtAdapter.bondedDevices)
            }else{
                dispEmparejadosLista = mBtAdapter.bondedDevices
            }

        }


        /**
         * ponemos los dipositivos en el listView solo valiada para opcion
         * A)
         */
        private fun viewDips(dispEmparejados: MutableSet<BluetoothDevice>) {
            if(dispEmparejados.size>0){
                for (device in dispEmparejados){
                    mDevicesArrayAdapter.add(device.name+"\n"+device.address)
                }
                listView!!.adapter = mDevicesArrayAdapter
            }else{
                mDevicesArrayAdapter.add("no existen dispositivos vinculados")
            }
        }

        /**
         *Opcion B) se retorna los dispositivos emparejados
         * para asi puedan hacer un customListView
         */
        fun dispEmparejados():ArrayList<String>{
            val arrayList = ArrayList<String>()
            if(dispEmparejadosLista.size>0){
                for (device in dispEmparejadosLista){
                    arrayList.add(device.name+"|"+device.address+device)
                    println(device.name+" -- "+device.address)
                }
            }else{
                arrayList.add("no existen dispositivos vinculados")
            }
            return  arrayList
        }


        /**
         *Valida para opcion -> (A)
         */
        fun bluetoothSeleccion(position: Int){

            //la direccion tiene 17 numeros
            val content = listView!!.getItemAtPosition(position).toString()
            address = content.substring(content.length-17)
            val sp  = ctx.getSharedPreferences("direccion", Activity.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("address", address)
            editor.apply()
            val intent = Intent(ctx,segundaActivity)
            ctx.startActivity(intent)
        }

        /**
         *Valida para opcion -> (B)
         */
        fun bluetoothSeleccionAddres(address:String){
            val intent = Intent(ctx,segundaActivity)
            val sp  = ctx.getSharedPreferences("direccion", Activity.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("address", address)
            editor.apply()
            ctx.startActivity(intent)


        }

        /**
         * Constructor 2 -----------------------------------------------
         */


        private var btSocket:BluetoothSocket? = null
        private var meInStream: InputStream? = null
        private var msOuStream: OutputStream? = null
        private lateinit var ctx2 :Context
        private var exitError = true
        private var mensajeError = "La conexion fallo"
        private var mensajeConectado = "jhr"




        private val BTMODULEUUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")


        /**
         * Funcion retorna true cuando se ha conectado el dispositivo
         */

        fun conectaBluetooth() :Boolean{

            mBtAdapter = BluetoothAdapter.getDefaultAdapter()
            var sp: SharedPreferences = ctx2.getSharedPreferences("direccion", Activity.MODE_PRIVATE)
            var direccion = sp.getString("address", "")
            val dispositivo = mBtAdapter.getRemoteDevice(direccion)
            try {
                btSocket = crearSocket(device = dispositivo)
            } catch (e: IOException) {
                Toast.makeText(ctx2,"La creacion del socket fallo",Toast.LENGTH_LONG).show()
            }

            try {
                btSocket!!.connect()
            } catch (var5: IOException) {
                try {
                    btSocket!!.close()
                } catch (var4: IOException) {
                    Toast.makeText(ctx2,"algo salio mal",Toast.LENGTH_LONG).show()
                }
            }
            connectedThread(btSocket)
            inicioConexion = true
            //mTx(mensajeConectado)
            return inicioConexion
        }


        /**
         * Retorna si ya inicio la conexion
         */
        fun inicioConexion(): Boolean {
            return inicioConexion
        }

        /**
         * Inicializamos variables
         */
        private fun connectedThread(socket: BluetoothSocket?){
            var DatosIn: InputStream? = null
            var DatosOut: OutputStream? = null
            try {
                DatosIn = socket!!.inputStream
                DatosOut = socket.outputStream
            } catch (var6: IOException) {
            }
            meInStream = DatosIn
            msOuStream = DatosOut
        }

        /**
         * Funcion que recepciona datos
         * provenientes de arduino
         */
        fun mRx() :String {
            try{
                val memoriaTemporal = ByteArray(256)
                val bytes = meInStream!!.read(memoriaTemporal)
                mensaje += String(memoriaTemporal, 0, bytes)
            }catch (var4:IOException){
                mensaje = "error"
                // si se quiere que se salga cuando exite error entonces se activa exitError
                // sino se deja exitError en false
                if (exitError){
                    inicioConexion = false
                    ctx2.startActivity(Intent(ctx2,activityAnterior1))
                }
            }
            return mensaje
        }

        /**
         * Resetea mensaje
         */
        fun mensajeReset(){
            mensaje = ""
        }


        /**
         *Funcion que envia datos a arduino
         */
        fun mTx(Entrada: String) {
            val MensajeBuffer = Entrada.toByteArray()
            try {
                msOuStream!!.write(MensajeBuffer)
            } catch (var10: IOException) {
                Toast.makeText(ctx2, mensajeError, Toast.LENGTH_LONG).show()
                inicioConexion = false
                var finaliza = Intent(ctx2, activityAnterior1)
               ctx2.startActivity(finaliza)
                try {
                    btSocket!!.close()
                } catch (var9: IOException) {
                    try {
                        btSocket!!.close()
                    } catch (var8: IOException) {
                    }
                }
            }
        }

        /**
         *Funcion que termina la
         *conexion bluetooth
         */
        fun exitConexion(){
            btSocket!!.close()
        }

        /**
         *Mensaje de error cuando se envia
         * datos y no esta conectado a un dispositivo
         */
        fun mensajeErrorTx(string: String){
            mensajeError = string
        }

        /**
         *Mensaje que se envia a arduino cuando este se
         * conecta exitosamente
         */
        fun mensajeConexion(string: String){
            mensajeConectado = string
        }

        /**
         *Visualizar mensaje de error ?
         */
        fun exitErrorOk(boolean: Boolean){
            exitError = boolean
        }

        /**
         *reglas para servicio
         */
        @Throws(IOException::class)
        private fun crearSocket(device: BluetoothDevice): BluetoothSocket? {
            return device.createRfcommSocketToServiceRecord(BTMODULEUUID)
        }

    }









}