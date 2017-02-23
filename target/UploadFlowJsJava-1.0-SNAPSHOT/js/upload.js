/*global angular */
'use strict';

/**
 * The main app module
 * 
 * @name upload
 * @type {angular.Module}
 */
(function() {
	/*
	 * You can use CORS changing the targe be sure to specify the domain in the UploadServlet file (java)
	 */
	var upload = angular.module('UploadModule', [,'flow' ]).config(
			[ 'flowFactoryProvider', function(flowFactoryProvider) {
				flowFactoryProvider.defaults = {
					target : '/upload',
					//target : 'https://26.2.169.56/organizzazione-di-test/territorio/upload/upload',
					permanentErrors : [ 500, 501 ],
					maxChunkRetries : 3,
					chunkRetryInterval : 5000,
					simultaneousUploads : 1,
					progressCallbacksInterval : 1,
					withCredentials : true,
					//testChunks : false,
					method : "octet"
				};
				flowFactoryProvider.on('catchAll', function(event) {
					console.log('catchAll', arguments);
				});
				// Can be used with different implementations of Flow.js
				// flowFactoryProvider.factory = fustyFlowFactory;
			} ]);

	upload.controller('ButtonController', function() {
		this.pausa = false;
		this.cancel = false;
				
		this.estadoPausa = function (file){
			if(file.paused || file.isComplete()){
				this.pause = false;
				return this.pause;
			}
			
			if(file.isUploading()){
				this.pause = true;
				return this.pause;
			}
			
		}
		this.estadoCancel = function (file){
			file.isUploading();
			return this.subido;
		}
		
	});
	
})();