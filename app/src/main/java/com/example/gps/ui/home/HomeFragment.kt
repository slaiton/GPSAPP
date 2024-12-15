package com.example.gps.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.gps.R

import com.example.gps.databinding.FragmentHomeBinding



class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val sharedPreferences: SharedPreferences =
            activity?.getSharedPreferences("LocationPreferences", Context.MODE_PRIVATE) ?: throw IllegalStateException("Activity is not attached")

       val viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        data class LocationData1(
            val codigo_usuario: String?,
            val lat: Double,
            val lon: Double
        )

        val status  = sharedPreferences.getBoolean("status", false)


            if (status) {
                binding.buttonText.text = "Activo"
                binding.buttonBackground.setImageResource(R.drawable.circle_button)
            } else {
                binding.buttonText.text = "Compartir"
                binding.buttonBackground.setImageResource(R.drawable.circle_off)
            }


        // Configura el botón de enviar ubicación
        binding.sendLocationButton.setOnClickListener {
            val status  = sharedPreferences.getBoolean("status", false)
            if (viewModel.isGpsEnabled(requireContext())) {
                if (!status){
                    viewModel.toggleLocationUpdates(sharedPreferences, requireActivity(), true)
                    binding.buttonText.text = "Activo"
                    binding.buttonBackground.setImageResource(R.drawable.circle_button)
                }else{
                    viewModel.toggleLocationUpdates(sharedPreferences, requireActivity(), false)
                    binding.buttonText.text = "Compartir"
                    binding.buttonBackground.setImageResource(R.drawable.circle_off)
                }
            } else {
                binding.buttonText.text = "GPS Desactivado"
            }
        }

        /**
        binding.mapButton.setOnClickListener {
            val userId = sharedPreferences.getString("user", "")
            if (!userId.isNullOrEmpty()) {
                val action = HomeFragmentDirections.actionHomeToMap(userId)
                findNavController().navigate(action)
            }
        }


        // Botón de cerrar sesión
        binding.logoutButton.setOnClickListener {
            sharedPreferences.edit().remove("savedCode").apply()
            findNavController().navigate(HomeFragmentDirections.actionHomeToLogin())
        }

         **/
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}