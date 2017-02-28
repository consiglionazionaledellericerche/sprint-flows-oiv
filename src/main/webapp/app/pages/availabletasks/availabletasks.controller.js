(function() {
  'use strict';

  angular
  .module('sprintApp')
  .controller('AvailableTasksController', AvalableTasksController);

  AvalableTasksController.$inject = ['$scope', 'Principal', 'LoginService', '$state', 'dataService', '$log'];

  function AvalableTasksController ($scope, Principal, LoginService, $state, dataService, $log) {
    var vm = this;
    vm.myTasks = {total: 0};
    vm.pooledTasks = {total: 0};

    $scope.loadTasks = function() {
      dataService.tasks.myTasksAvailable()
      .then(function (response) {
          vm.pooledTasks = response.data;
        }, function (response) {
          $log.error(response);
        });
      dataService.tasks.myTasks()
      .then(function (response) {
          vm.myTasks = response.data
        }, function (response) {
          $log.error(response);
        });
    }

    $scope.loadTasks();

    $scope.setActiveContent = function(choice) {
        $scope.activeContent = choice;
    }
  }


})();
